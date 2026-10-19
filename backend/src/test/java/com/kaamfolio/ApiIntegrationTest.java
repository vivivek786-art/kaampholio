package com.kaamfolio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:kaamfolio;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "app.password=test-password-123", "app.username=owner", "app.upload-dir=target/test-uploads"})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    ObjectNode state() throws Exception {
        return (ObjectNode)json.readTree(mvc.perform(get("/api/me/state").with(httpBasic("owner","test-password-123"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void rejectsAnonymousAndWrongPassword() throws Exception {
        mvc.perform(get("/api/me/state")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/state").with(httpBasic("owner","wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void seededProfilesAreReturned() throws Exception {
        mvc.perform(get("/api/professionals").with(httpBasic("owner","test-password-123")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
    }
    @Test void profileFavoritesPersistenceAndStaleRevision() throws Exception {
        ObjectNode s=state(); ((ObjectNode)s.get("profile")).put("name","Sagar Rana"); s.putArray("saved").add("raj");
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.profile.name").value("Sagar Rana")).andExpect(jsonPath("$.saved[0]").value("raj"));
        assertEquals("Sagar Rana",state().path("profile").path("name").asText());
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString())).andExpect(status().isConflict());
    }
    @Test void invalidProfileCannotChangeDatabase() throws Exception {
        ObjectNode s=state(); String before=s.toString(); ((ObjectNode)s.get("profile")).put("rate",-1);
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString())).andExpect(status().isBadRequest());
        assertEquals(before,state().toString());
    }
    @Test void invalidFavoriteRollsBackProfileChange() throws Exception {
        ObjectNode s=state(); String before=s.toString(); ((ObjectNode)s.get("profile")).put("name","Do not save"); s.putArray("saved").add("nonexistent");
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString())).andExpect(status().isNotFound());
        assertEquals(before,state().toString());
    }
    @Test void uploadSaveReadRemovePortfolio() throws Exception {
        byte[] png=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Z1S8AAAAASUVORK5CYII=");
        var upload=new MockMultipartFile("file","test.png","image/png",png);
        String body=mvc.perform(multipart("/api/media").file(upload).with(httpBasic("owner","test-password-123"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String src=json.readTree(body).get("src").asText();
        mvc.perform(get(src)).andExpect(status().isOk()).andExpect(content().bytes(png));
        var s=state(); var w=((ObjectNode)s.get("profile")).putArray("work").addObject();
        w.put("id","integration-work").put("title","Test project").put("description","A portfolio upload").put("kind","image").put("src",src).putArray("tools").add("Drill");
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString())).andExpect(status().isOk());
        mvc.perform(delete("/api"+src).with(httpBasic("owner","test-password-123"))).andExpect(status().isConflict());
        s=state(); ((ObjectNode)s.get("profile")).putArray("work");
        mvc.perform(put("/api/me/state").with(httpBasic("owner","test-password-123")).contentType(MediaType.APPLICATION_JSON).content(s.toString())).andExpect(status().isOk());
        mvc.perform(delete("/api"+src).with(httpBasic("owner","test-password-123"))).andExpect(status().isNoContent());
        mvc.perform(get(src)).andExpect(status().isNotFound());
    }
    @Test void rejectsDisguisedHtmlUpload() throws Exception {
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file","bad.png","image/png","<script>alert(1)</script>".getBytes())).with(httpBasic("owner","test-password-123"))).andExpect(status().isBadRequest());
    }
}
