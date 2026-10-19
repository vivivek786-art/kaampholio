package com.kaamfolio;

import com.kaamfolio.Models.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ApiController {
	private final PortfolioService portfolio;
	private final MediaService media;
	private final JdbcTemplate db;

	public ApiController(PortfolioService portfolio, MediaService media, JdbcTemplate db) {
		this.portfolio = portfolio;
		this.media = media;
		this.db = db;
	}

	@GetMapping("/api/health")
	public Map<String, String> health() {
		db.queryForObject("SELECT 1", Integer.class);
		return Map.of("status", "UP");
	}

	@GetMapping("/api/professionals")
	public List<Professional> professionals() {
		return portfolio.professionals();
	}

	@GetMapping("/api/professionals/{id}")
	public Professional professional(@PathVariable String id) {
		return portfolio.professional(id);
	}

	@GetMapping("/api/me/state")
	public State state() {
		return portfolio.state();
	}

	@PutMapping("/api/me/state")
	public State save(@Valid @RequestBody State state) {
		return portfolio.save(state);
	}

	@PostMapping(value = "/api/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
		return Map.of("src", media.upload(file));
	}

	@DeleteMapping("/api/media/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String name) throws IOException {
		if (db.queryForObject("SELECT COUNT(*) FROM portfolio_work WHERE src=?", Integer.class, "/media/" + name) > 0)
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove the project before deleting its media");
		media.delete(name);
	}

	@GetMapping("/media/{name}")
	public ResponseEntity<FileSystemResource> media(@PathVariable String name) {
		var resource = new FileSystemResource(media.path(name));
		if (!resource.exists())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.type(name)))
				.header("X-Content-Type-Options", "nosniff").body(resource);
	}
}
