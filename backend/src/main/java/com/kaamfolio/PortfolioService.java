package com.kaamfolio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaamfolio.Models.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class PortfolioService {
	private final JdbcTemplate db;
	private final ObjectMapper json;
	private final MediaService media;

	public PortfolioService(JdbcTemplate db, ObjectMapper json, MediaService media) {
		this.db = db;
		this.json = json;
		this.media = media;
	}

	private String encode(List<String> value) {
		try {
			return json.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private List<String> decode(String value) {
		try {
			return json.readValue(value, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public List<Professional> professionals() {
		return db.queryForList("SELECT id FROM professionals WHERE id <> 'me' ORDER BY name", String.class).stream()
				.map(this::professional).toList();
	}

	public Professional professional(String id) {
		var work = db.query("SELECT * FROM portfolio_work WHERE professional_id=? ORDER BY sort_order",
				(r, n) -> new Work(r.getString("id"), r.getString("title"), r.getString("description"),
						r.getString("kind"), r.getString("src"), decode(r.getString("tools"))),
				id);
		return db
				.query("SELECT * FROM professionals WHERE id=?",
						(r, n) -> new Professional(r.getString("id"), r.getString("name"), r.getString("trade"),
								r.getString("city"), r.getString("area"), r.getInt("experience"), r.getDouble("rating"),
								r.getInt("reviews"), r.getInt("rate"), r.getBoolean("available"),
								decode(r.getString("skills")), decode(r.getString("tools")), r.getString("bio"), work),
						id)
				.stream().findFirst()
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Professional not found"));
	}

	@Transactional(readOnly = true)
	public State state() {
		return new State(1, db.queryForObject("SELECT revision FROM state_revision WHERE owner_id='me'", Long.class),
				professional("me"),
				db.queryForList(
						"SELECT professional_id FROM saved_professionals WHERE owner_id='me' ORDER BY professional_id",
						String.class));
	}

	@Transactional
	public State save(State state) {
		var p = state.profile();
		if (!p.id().equals("me"))
			throw new ResponseStatusException(BAD_REQUEST, "Only your own profile can be edited");
		long revision = db.queryForObject("SELECT revision FROM state_revision WHERE owner_id='me' FOR UPDATE",
				Long.class);
		if (revision != state.revision())
			throw new ResponseStatusException(CONFLICT, "Profile changed in another window. Reload before editing.");
		if (p.work().stream().map(Work::id).distinct().count() != p.work().size())
			throw new ResponseStatusException(BAD_REQUEST, "Duplicate work IDs");
		for (var w : p.work()) {
			media.validateReference(w.src(), w.kind());
			if (db.queryForObject("SELECT COUNT(*) FROM portfolio_work WHERE id=? AND professional_id<>'me'",
					Integer.class, w.id()) > 0)
				throw new ResponseStatusException(BAD_REQUEST, "Work ID already exists");
		}
		for (String id : state.saved())
			professional(id);
		// Rating and reviews are read-only sample data; clients cannot award themselves
		// ratings.
		db.update(
				"UPDATE professionals SET name=?,trade=?,city=?,area=?,experience=?,rate=?,available=?,skills=?,tools=?,bio=? WHERE id='me'",
				p.name().trim(), p.trade(), p.city().trim(), p.area().trim(), p.experience(), p.rate(), p.available(),
				encode(p.skills()), encode(p.tools()), p.bio());
		db.update("DELETE FROM portfolio_work WHERE professional_id='me'");
		int position = 0;
		for (var w : p.work())
			db.update("INSERT INTO portfolio_work VALUES (?,?,?,?,?,?,?,?)", w.id(), "me", w.title(), w.description(),
					w.kind(), w.src(), encode(w.tools()), position++);
		db.update("DELETE FROM saved_professionals WHERE owner_id='me'");
		for (String id : new LinkedHashSet<>(state.saved()))
			db.update("INSERT INTO saved_professionals VALUES ('me',?)", id);
		db.update("UPDATE state_revision SET revision=revision+1 WHERE owner_id='me'");
		return state();
	}
}
