package com.infosys.util.health;

import com.infosys.elastic.helper.ConnectionManager;
import org.apache.http.StatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ElasticsearchRestHealthIndicator extends AbstractHealthIndicator {
	private static final String RED_STATUS = "red";
	private final RestClient client;
	private final JsonParser jsonParser;

	public ElasticsearchRestHealthIndicator() {
		super("Elasticsearch health check failed");
		this.client = ConnectionManager.getClient().getLowLevelClient();
		this.jsonParser = JsonParserFactory.getJsonParser();
	}

	protected void doHealthCheck(Builder builder) throws Exception {
		Response response = this.client.performRequest(new Request("GET", "/_cluster/health/"));
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() != 200) {
			builder.down();
			builder.withDetail("statusCode", statusLine.getStatusCode());
			builder.withDetail("reasonPhrase", statusLine.getReasonPhrase());
		} else {
			InputStream inputStream = response.getEntity().getContent();
			Throwable var5 = null;

			try {
				this.doHealthCheck(builder, StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
			} catch (Throwable var14) {
				var5 = var14;
				throw var14;
			} finally {
				if (inputStream != null) {
					if (var5 != null) {
						try {
							inputStream.close();
						} catch (Throwable var13) {
							var5.addSuppressed(var13);
						}
					} else {
						inputStream.close();
					}
				}

			}

		}
	}

	private void doHealthCheck(Builder builder, String json) {
		Map<String, Object> response = this.jsonParser.parseMap(json);
		String status = (String)response.get("status");
		if ("red".equals(status)) {
			builder.outOfService();
		} else {
			builder.up();
		}
		response.forEach(builder::withDetail);
	}
}
