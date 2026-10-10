package com.example.bookmanagement.functional

import com.fasterxml.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/** HTTPからPostgreSQLまでを通してAPIの主要シナリオを検証するfunctionalテスト。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookManagementFunctionalTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
    @LocalServerPort private val port: Int,
) {
    @BeforeEach
    fun resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE book_authors, books, authors RESTART IDENTITY")
    }

    /**
     * 著者の登録、更新、再取得がHTTP APIを通して一貫して行えることを確認する。
     */
    @Test
    fun `creates and updates an author through the HTTP API`() {
        val created = post("/authors", authorRequest("夏目漱石", LocalDate.now(clock)))

        assertEquals(HttpStatus.CREATED, created.statusCode)
        assertEquals("夏目漱石", body(created).required("name").asText())
        val authorId = body(created).required("id").asLong()

        val updated = put("/authors/$authorId", authorRequest("夏目漱石（更新）", LocalDate.now(clock)))

        assertEquals(HttpStatus.OK, updated.statusCode)
        assertEquals("夏目漱石（更新）", body(updated).required("name").asText())
        assertEquals("夏目漱石（更新）", body(get("/authors/$authorId")).required("name").asText())
    }

    /**
     * 未来の生年月日、負の価格、著者なし、存在しない著者を指定した書籍登録が400となることを確認する。
     */
    @Test
    fun `rejects invalid author and book requests through the HTTP API`() {
        val futureBirthDate = post("/authors", authorRequest("未来の著者", LocalDate.now(clock).plusDays(1)))
        assertBadRequest(futureBirthDate, "birthDate must not be in the future")

        val negativePrice = post(
            "/books",
            bookRequest(price = BigDecimal("-1"), authorIds = setOf(1)),
        )
        assertBadRequest(negativePrice, "price must be greater than or equal to 0")

        val noAuthors = post("/books", bookRequest(authorIds = emptySet()))
        assertBadRequest(noAuthors, "authorIds must contain at least one author")

        val missingAuthor = post("/books", bookRequest(authorIds = setOf(999)))
        assertBadRequest(missingAuthor, "Authors not found: [999]")
    }

    /**
     * 複数著者の書籍を登録後、更新で関連著者を全置換し、著者別書籍一覧へ反映されることを確認する。
     */
    @Test
    fun `creates a multi-author book updates it and returns books by author`() {
        val firstAuthorId = createAuthor("著者A")
        val secondAuthorId = createAuthor("著者B")
        val created = post("/books", bookRequest(authorIds = setOf(firstAuthorId, secondAuthorId)))

        assertEquals(HttpStatus.CREATED, created.statusCode)
        assertEquals(2, body(created).required("authors").size())
        val bookId = body(created).required("id").asLong()

        val updated = put(
            "/books/$bookId",
            bookRequest(
                title = "更新後の書籍",
                publicationStatus = "PUBLISHED",
                authorIds = setOf(secondAuthorId),
            ),
        )

        assertEquals(HttpStatus.OK, updated.statusCode)
        assertEquals("PUBLISHED", body(updated).required("publicationStatus").asText())
        assertEquals(listOf(secondAuthorId), body(updated).required("authors").map { it.required("id").asLong() })
        assertEquals(0, body(get("/authors/$firstAuthorId/books")).size())
        assertEquals(listOf(bookId), body(get("/authors/$secondAuthorId/books")).map { it.required("id").asLong() })
    }

    /**
     * 出版済みの書籍を未出版へ戻す更新が業務ルール違反として400になることを確認する。
     */
    @Test
    fun `rejects changing a published book back to unpublished`() {
        val authorId = createAuthor("著者")
        val bookId = body(post("/books", bookRequest(authorIds = setOf(authorId)))).required("id").asLong()
        val published = put("/books/$bookId", bookRequest(publicationStatus = "PUBLISHED", authorIds = setOf(authorId)))
        assertEquals(HttpStatus.OK, published.statusCode)

        val unpublished = put("/books/$bookId", bookRequest(authorIds = setOf(authorId)))

        assertBadRequest(unpublished, "PUBLISHED books cannot be changed to UNPUBLISHED")
    }

    /**
     * 存在しない著者・書籍に対する取得および更新が404になることを確認する。
     */
    @Test
    fun `returns 404 for missing resources`() {
        assertEquals(HttpStatus.NOT_FOUND, get("/authors/999").statusCode)
        assertEquals(HttpStatus.NOT_FOUND, get("/books/999").statusCode)
        assertEquals(HttpStatus.NOT_FOUND, put("/authors/999", authorRequest("著者", LocalDate.now(clock))).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, put("/books/999", bookRequest(authorIds = setOf(1))).statusCode)
    }

    private fun createAuthor(name: String): Long =
        body(post("/authors", authorRequest(name, LocalDate.now(clock)))).required("id").asLong()

    private fun authorRequest(name: String, birthDate: LocalDate): Map<String, Any> = mapOf(
        "name" to name,
        "birthDate" to birthDate.toString(),
    )

    private fun bookRequest(
        title: String = "書籍",
        price: BigDecimal = BigDecimal.ZERO,
        publicationStatus: String = "UNPUBLISHED",
        authorIds: Set<Long>,
    ): Map<String, Any> = mapOf(
        "title" to title,
        "price" to price,
        "publicationStatus" to publicationStatus,
        "authorIds" to authorIds,
    )

    private fun post(path: String, body: Any): ResponseEntity<JsonNode> =
        exchange(HttpMethod.POST, path, body)

    private fun put(path: String, body: Any): ResponseEntity<JsonNode> =
        exchange(HttpMethod.PUT, path, body)

    private fun get(path: String): ResponseEntity<JsonNode> =
        restTemplate.getForEntity(url(path), JsonNode::class.java)

    private fun exchange(method: HttpMethod, path: String, body: Any): ResponseEntity<JsonNode> =
        restTemplate.exchange(
            url(path),
            method,
            HttpEntity(body, org.springframework.http.HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
            JsonNode::class.java,
        )

    private fun assertBadRequest(response: ResponseEntity<JsonNode>, message: String) {
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(message, body(response).required("message").asText())
    }

    private fun body(response: ResponseEntity<JsonNode>): JsonNode = response.body ?: error("Response body is required")

    private fun url(path: String): String = "http://localhost:$port$path"

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
