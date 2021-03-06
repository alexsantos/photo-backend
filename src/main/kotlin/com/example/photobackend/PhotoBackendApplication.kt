package com.example.photobackend

import com.google.cloud.spring.data.datastore.core.mapping.Entity
import com.google.cloud.spring.data.datastore.repository.DatastoreRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.core.io.WritableResource
import org.springframework.data.annotation.Id
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@SpringBootApplication
class PhotoBackendApplication

fun main(args: Array<String>) {
    runApplication<PhotoBackendApplication>(*args)
}

@Entity
data class Photo(
    @Id
    var id: String? = null,
    var uri: String? = null,
    var label: String? = null
)

@RepositoryRestResource
interface PhotoRepository : DatastoreRepository<Photo, String>

@RestController
class HelloController(
    private val photoRepository: PhotoRepository
) {
    @GetMapping("/")
    fun hello() = "hello!"

    @PostMapping("/photo")
    fun create(@RequestBody photo: Photo) {
        photoRepository.save(photo)
    }
}

@RestController
class UploadController(
    private val ctx: ApplicationContext,
    private val photoRepository: PhotoRepository
) {
    private val bucket = "gs://kotlin-labs-001-photos/images"

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): Photo {
        val id = UUID.randomUUID().toString()
        val uri = "$bucket/$id"

        val gcs = ctx.getResource(uri) as WritableResource

        file.inputStream.use { input ->
            gcs.outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return photoRepository.save(Photo(id = id, uri = "image/$id"))
    }

    @GetMapping("/image/{id}")
    fun get(@PathVariable id: String) : ResponseEntity<Resource> {
        val resource = ctx.getResource("$bucket/$id")
        return if (resource.exists()) {
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}