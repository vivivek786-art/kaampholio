package com.kaamfolio;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class MediaService {
	private final Path root;
	private static final Map<String, String> TYPES = Map.of("jpg", "image/jpeg", "png", "image/png", "webp",
			"image/webp", "mp4", "video/mp4", "mov", "video/quicktime");

	public MediaService(@Value("${app.upload-dir}") String dir) throws IOException {
		root = Path.of(dir).toAbsolutePath().normalize();
		Files.createDirectories(root);
	}

	public Path path(String name) {
		if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp|mp4|mov)"))
			throw new ResponseStatusException(NOT_FOUND, "Media not found");
		return root.resolve(name);
	}

	public String type(String name) {
		return TYPES.get(name.substring(name.lastIndexOf('.') + 1));
	}

	public String upload(MultipartFile file) throws IOException {
		if (file.isEmpty() || file.getSize() > 25L * 1024 * 1024)
			throw new ResponseStatusException(BAD_REQUEST, "Choose a nonempty file up to 25 MB");
		byte[] head;
		try (var in = file.getInputStream()) {
			head = in.readNBytes(16);
		}
		String ext = null;
		if (head.length >= 3 && (head[0] & 255) == 255 && (head[1] & 255) == 216 && (head[2] & 255) == 255)
			ext = "jpg";
		else if (head.length >= 8
				&& Arrays.equals(Arrays.copyOf(head, 8), new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 }))
			ext = "png";
		else if (head.length >= 12 && new String(head, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
				&& new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))
			ext = "webp";
		else if (head.length >= 12
				&& new String(head, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp")) {
			String brand = new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
			if (Set.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "MSNV").contains(brand))
				ext = "mp4";
			else if (brand.equals("qt  "))
				ext = "mov";
		}
		if (ext == null)
			throw new ResponseStatusException(BAD_REQUEST,
					"Supported formats: JPEG, PNG, WebP, MP4 and MOV. Convert HEIC images to JPEG first.");
		String name = UUID.randomUUID() + "." + ext;
		try (var in = file.getInputStream()) {
			Files.copy(in, path(name));
		}
		return "/media/" + name;
	}

	public void validateReference(String src, String kind) {
		if (!src.startsWith("/media/"))
			throw new ResponseStatusException(BAD_REQUEST, "Upload media before saving a project");
		String name = src.substring(7);
		var p = path(name);
		if (!Files.isRegularFile(p) || !type(name).startsWith(kind + "/"))
			throw new ResponseStatusException(BAD_REQUEST, "Missing media or media type mismatch");
	}

	public void delete(String name) throws IOException {
		Files.deleteIfExists(path(name));
	}
}
