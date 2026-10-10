package com.example.bookmanagement.integration

import com.example.bookmanagement.dto.AuthorRequest
import com.example.bookmanagement.dto.BookRequest
import com.example.bookmanagement.dto.BookResponse
import com.example.bookmanagement.exception.InvalidPublicationStatusTransitionException
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.service.AuthorService
import com.example.bookmanagement.service.BookService
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import javax.sql.DataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.jooq.exception.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryIntegrationTest @Autowired constructor(
    @Autowired private val authorService: AuthorService,
    @Autowired private val bookService: BookService,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val clock: Clock,
    @Autowired private val dataSource: DataSource,
) {
    @BeforeEach
    fun resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE book_authors, books, authors RESTART IDENTITY")
    }

    @AfterEach
    fun removeFailureTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_book_author_insert ON book_authors")
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_book_author_insert()")
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS block_published_book_update ON books")
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS block_published_book_update()")
    }

    @Test
    fun `persists and updates an author`() {
        val birthDate = LocalDate.now(clock)
        val created = authorService.create(AuthorRequest("著者", birthDate))

        assertEquals(created, authorService.get(created.id))

        val updated = authorService.update(created.id, AuthorRequest("更新後著者", birthDate))

        assertEquals("更新後著者", bookAuthorName(created.id))
        assertEquals(updated, authorService.get(created.id))
    }

    @Test
    fun `persists a book with multiple authors and replaces associations`() {
        val firstAuthor = createAuthor("著者A")
        val secondAuthor = createAuthor("著者B")
        val book = bookService.create(bookRequest(authorIds = setOf(firstAuthor, secondAuthor)))

        assertEquals(setOf(firstAuthor, secondAuthor), book.authors.map { it.id }.toSet())
        assertEquals(setOf(firstAuthor, secondAuthor), bookService.get(book.id).authors.map { it.id }.toSet())

        val updated = bookService.update(
            book.id,
            bookRequest(authorIds = setOf(secondAuthor), publicationStatus = PublicationStatus.PUBLISHED),
        )

        assertEquals(PublicationStatus.PUBLISHED, updated.publicationStatus)
        assertEquals(listOf(secondAuthor), updated.authors.map { it.id })
        assertEquals(listOf(book.id), bookService.findByAuthorId(secondAuthor).map { it.id })
        assertEquals(0, bookService.findByAuthorId(firstAuthor).size)
    }

    @Test
    fun `rolls back both book and associations when relation replacement fails`() {
        val firstAuthor = createAuthor("著者A")
        val secondAuthor = createAuthor("著者B")
        val original = bookService.create(bookRequest(title = "更新前", authorIds = setOf(firstAuthor)))
        installFailingBookAuthorTrigger()

        assertThrows(DataAccessException::class.java) {
            bookService.update(
                original.id,
                bookRequest(
                    title = "更新後",
                    authorIds = setOf(secondAuthor),
                    publicationStatus = PublicationStatus.PUBLISHED,
                ),
            )
        }

        val afterFailure = bookService.get(original.id)
        assertEquals("更新前", afterFailure.title)
        assertEquals(PublicationStatus.UNPUBLISHED, afterFailure.publicationStatus)
        assertEquals(listOf(firstAuthor), afterFailure.authors.map { it.id })
    }

    @Test
    fun `serializes concurrent publication status updates to prevent published to unpublished`() {
        val authorId = createAuthor("著者")
        val original = bookService.create(bookRequest(authorIds = setOf(authorId)))
        val advisoryLockId = 7_241L
        val executor = Executors.newFixedThreadPool(2)
        val publishedRequestStarted = CountDownLatch(1)
        val unpublishedRequestStarted = CountDownLatch(1)

        dataSource.connection.use { gateConnection ->
            gateConnection.createStatement().use { statement ->
                statement.execute("SELECT pg_advisory_lock($advisoryLockId)")
            }
            installPublishedUpdateBlockingTrigger(advisoryLockId)

            try {
                val publishFuture = executor.submit<BookResponse> {
                    publishedRequestStarted.countDown()
                    bookService.update(
                        original.id,
                        bookRequest(publicationStatus = PublicationStatus.PUBLISHED, authorIds = setOf(authorId)),
                    )
                }

                assertTrue(publishedRequestStarted.await(5, TimeUnit.SECONDS))
                waitForBookUpdateLock()

                val unpublishFuture = executor.submit {
                    unpublishedRequestStarted.countDown()
                    bookService.update(
                        original.id,
                        bookRequest(publicationStatus = PublicationStatus.UNPUBLISHED, authorIds = setOf(authorId)),
                    )
                }
                assertTrue(unpublishedRequestStarted.await(5, TimeUnit.SECONDS))
                waitForBlockedTransactionLock()

                gateConnection.createStatement().use { statement ->
                    statement.execute("SELECT pg_advisory_unlock($advisoryLockId)")
                }

                assertEquals(PublicationStatus.PUBLISHED, publishFuture.get(5, TimeUnit.SECONDS).publicationStatus)
                val failure = assertThrows(ExecutionException::class.java) {
                    unpublishFuture.get(5, TimeUnit.SECONDS)
                }
                assertTrue(failure.cause is InvalidPublicationStatusTransitionException)
                assertEquals(PublicationStatus.PUBLISHED, bookService.get(original.id).publicationStatus)
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun createAuthor(name: String): Long =
        authorService.create(AuthorRequest(name, LocalDate.now(clock))).id

    private fun bookRequest(
        title: String = "書籍",
        authorIds: Set<Long>,
        publicationStatus: PublicationStatus = PublicationStatus.UNPUBLISHED,
    ): BookRequest = BookRequest(
        title = title,
        price = BigDecimal.ZERO,
        publicationStatus = publicationStatus,
        authorIds = authorIds,
    )

    private fun bookAuthorName(authorId: Long): String =
        jdbcTemplate.queryForObject("SELECT name FROM authors WHERE id = ?", String::class.java, authorId)
            ?: error("Author $authorId was not found")

    private fun installFailingBookAuthorTrigger() {
        jdbcTemplate.execute(
            """
            CREATE FUNCTION fail_book_author_insert() RETURNS trigger AS $$
            BEGIN
                RAISE EXCEPTION 'forced book_authors insert failure';
            END;
            $$ LANGUAGE plpgsql;
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            """
            CREATE TRIGGER fail_book_author_insert
            BEFORE INSERT ON book_authors
            FOR EACH ROW EXECUTE FUNCTION fail_book_author_insert();
            """.trimIndent(),
        )
    }

    private fun installPublishedUpdateBlockingTrigger(advisoryLockId: Long) {
        jdbcTemplate.execute(
            """
            CREATE FUNCTION block_published_book_update() RETURNS trigger AS $$
            BEGIN
                IF NEW.publication_status = 'PUBLISHED' THEN
                    PERFORM pg_advisory_xact_lock($advisoryLockId);
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            """
            CREATE TRIGGER block_published_book_update
            BEFORE UPDATE OF publication_status ON books
            FOR EACH ROW EXECUTE FUNCTION block_published_book_update();
            """.trimIndent(),
        )
    }

    private fun waitForBookUpdateLock() {
        repeat(500) {
            val updateLockHeld = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_locks locks
                    JOIN pg_class relation ON relation.oid = locks.relation
                    WHERE relation.relname = 'books'
                      AND locks.mode = 'RowExclusiveLock'
                      AND locks.granted
                )
                """.trimIndent(),
                Boolean::class.java,
            ) ?: false
            if (updateLockHeld) {
                return
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
        }
        error("Timed out waiting for the PUBLISHED update to acquire the book row lock")
    }

    private fun waitForBlockedTransactionLock() {
        repeat(500) {
            val transactionLockBlocked = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_locks
                    WHERE locktype = 'transactionid'
                      AND NOT granted
                )
                """.trimIndent(),
                Boolean::class.java,
            ) ?: false
            if (transactionLockBlocked) {
                return
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
        }
        error("Timed out waiting for the UNPUBLISHED update to block on the book row lock")
    }

    companion object {
        @JvmField
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun configureDatabase(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
