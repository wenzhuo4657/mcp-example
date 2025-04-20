/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.chat.advisors;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * Incorporate DeepSeek-R1's reasoning content into the output
 */

public class ReasoningContentAdvisor implements BaseAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(ReasoningContentAdvisor.class);

	private final int order;

	public ReasoningContentAdvisor(Integer order) {
		this.order = order != null ? order : 0;
	}

	@NotNull
	@Override
	public AdvisedRequest before(@NotNull AdvisedRequest request) {

		return request;
	}

	@NotNull
	@Override
	public AdvisedResponse after(AdvisedResponse advisedResponse) {

		ChatResponse resp = advisedResponse.response();
		if (Objects.isNull(resp)) {

			return advisedResponse;
		}

		logger.debug(String.valueOf(resp.getResults().get(0).getOutput().getMetadata()));
		String reasoningContent = String.valueOf(resp.getResults().get(0).getOutput().getMetadata().get("reasoningContent"));

		if (StringUtils.hasText(reasoningContent)) {
			List<Generation> thinkGenerations = resp.getResults().stream()
					.map(generation -> {
						AssistantMessage output = generation.getOutput();
						AssistantMessage thinkAssistantMessage = new AssistantMessage(
									String.format("<think>%s</think>", reasoningContent) + output.getText(),
								output.getMetadata(),
								output.getToolCalls(),
								output.getMedia()
						);
						return new Generation(thinkAssistantMessage, generation.getMetadata());
					}).toList();

			ChatResponse thinkChatResp = ChatResponse.builder().from(resp).generations(thinkGenerations).build();
			return AdvisedResponse.from(advisedResponse).response(thinkChatResp).build();

		}

		return advisedResponse;
	}

	@Override
	public int getOrder() {

		return this.order;
	}

}