/*
 * Copyright 2020 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.armory.plugin.events.listener.newrelic

import com.netflix.spinnaker.echo.api.events.Event
import com.netflix.spinnaker.echo.api.events.EventListener
import org.slf4j.LoggerFactory
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger

class NewRelicEventListenerPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val logger = LoggerFactory.getLogger(NewRelicEventListenerPlugin::class.java)

    override fun start() {
        logger.info("NewRelicEventListenerPlugin.start()")
    }

    override fun stop() {
        logger.info("NewRelicEventListenerPlugin.stop()")
    }
}

data class NewRelicEvent(
        val eventType: String,
        val text: String,
        val priority: String,
        val tags: Set<String>,
        val alert_type: String
)

@Extension
open class NewRelicEventListener(val configuration: NewRelicEventListenerConfig) : EventListener {

    private val log = LoggerFactory.getLogger(NewRelicEventListener::class.java)

    private val mapper = jacksonObjectMapper()

    protected open fun getHttpClient() : OkHttpClient {
        return OkHttpClient()
    }

    protected open fun getLogger() : Logger {
        return log
    }

    override fun processEvent(event: Event) {

        val tags = mutableSetOf(
                "source:${event.details.source}",
                "eventType:${event.details.type}",
                "application:${event.details.application}"
        )
        val execution = event.content["execution"] as Map<String, Any?>
        tags.add("executionType:${execution["type"]}")
        tags.add("executionStatus:${execution["status"]}")
        tags.add("executionId:${execution["id"]}")
        execution["name"]?.let {
            tags.add("pipelineName:$it")
        }
        execution["pipelineConfigId"]?.let {
            tags.add("pipelineConfigId:$it")
        }

        val newRelicEvent = NewRelicEvent(
                configuration.eventType,
                mapper.writeValueAsString(event),
                "normal",
                tags,
                "info"
        )
        val newRelicEventJson = mapper.writeValueAsString(newRelicEvent)

        val body = RequestBody.create(
                MediaType.parse("application/json"), newRelicEventJson)
        val request = Request.Builder()
                .url("https://insights-collector.newrelic.com/v1/accounts/${configuration.account}/events")
                .addHeader("X-Insert-Key", configuration.apiKey)
                .post(body)
                .build()
        val call = getHttpClient().newCall(request)
        val response = call.execute()

        if (!response.isSuccessful) {
            getLogger().error("NewRelic event listener failed with response: ${response.toString()}")
        }
    }
}
