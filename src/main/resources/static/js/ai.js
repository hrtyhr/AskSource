(function () {
    "use strict";

    var form = document.getElementById("chatForm");
    if (!form) {
        return;
    }

    var input = document.getElementById("chatInput");
    var sendButton = document.getElementById("sendButton");
    var chatLog = document.getElementById("chatLog");
    var welcome = document.getElementById("chatWelcome");
    var panel = document.getElementById("chatPanel");
    var conversationList = document.getElementById("conversationList");
    var enabled = form.dataset.enabled !== "false";
    var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute("content");
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute("content");
    var conversationId = panel.dataset.conversationId ? Number(panel.dataset.conversationId) : null;
    var streaming = false;

    function scrollToBottom() {
        if (chatLog) {
            chatLog.scrollTop = chatLog.scrollHeight;
        }
    }

    function hideWelcome() {
        if (welcome) {
            welcome.remove();
            welcome = null;
        }
    }

    function makeMessage(role, text) {
        var article = document.createElement("div");
        article.className = "message " + role;
        var avatar = document.createElement("div");
        avatar.className = "message-avatar";
        avatar.textContent = role === "user" ? "你" : "AI";
        var body = document.createElement("div");
        body.className = "message-body";
        if (role === "user") {
            body.textContent = text;
        } else {
            body.classList.add("streaming-cursor");
            var textNode = document.createElement("div");
            textNode.className = "message-text";
            body.appendChild(textNode);
        }
        article.appendChild(avatar);
        article.appendChild(body);
        chatLog.appendChild(article);
        scrollToBottom();
        return { article: article, body: body };
    }

    function renderSources(container, sources) {
        if (!sources || !sources.length) {
            return;
        }
        var row = document.createElement("div");
        row.className = "message-sources";
        sources.forEach(function (source) {
            var link = document.createElement("a");
            link.className = "message-source";
            link.href = source.url;
            link.textContent = (source.sourceType === "REPLY" ? "回复 · " : "帖子 · ") + source.title;
            row.appendChild(link);
        });
        container.appendChild(row);
    }

    function renderMessages(messages) {
        chatLog.innerHTML = "";
        messages.forEach(function (message) {
            var article = document.createElement("div");
            article.className = "message " + message.role;
            var avatar = document.createElement("div");
            avatar.className = "message-avatar";
            avatar.textContent = message.role === "user" ? "你" : "AI";
            var body = document.createElement("div");
            body.className = "message-body";
            body.innerHTML = message.html;
            article.appendChild(avatar);
            article.appendChild(body);
            renderSources(body, message.sources);
            chatLog.appendChild(article);
        });
        scrollToBottom();
    }

    async function refreshMessages() {
        if (conversationId == null) {
            return;
        }
        try {
            var response = await fetch("/api/ai/conversations/" + conversationId + "/messages", {
                headers: { "Accept": "application/json" }
            });
            if (response.ok) {
                renderMessages(await response.json());
            }
        } catch (error) {
            console.warn("刷新消息失败", error);
        }
    }

    function addConversation(title) {
        if (!conversationList || conversationId == null) {
            return;
        }
        var existing = conversationList.querySelector('[data-conversation-id="' + conversationId + '"]');
        if (existing) {
            return;
        }
        var empty = conversationList.querySelector(".conversation-empty");
        if (empty) {
            empty.remove();
        }
        var link = document.createElement("a");
        link.className = "conversation-item is-active";
        link.dataset.conversationId = String(conversationId);
        link.href = "/ai?conversationId=" + conversationId;
        link.textContent = title || "新对话";
        var time = document.createElement("time");
        time.textContent = "刚刚";
        link.appendChild(time);
        conversationList.insertBefore(link, conversationList.firstChild);
    }

    function removeEmptyAssistant() {
        var last = chatLog.querySelector(".message.assistant:last-child");
        if (!last) {
            return;
        }
        var text = last.querySelector(".message-text");
        var sources = last.querySelector(".message-sources");
        if ((!text || !text.textContent.trim()) && !sources) {
            last.remove();
        }
    }

    function finishStream() {
        streaming = false;
        if (sendButton) {
            sendButton.disabled = false;
            sendButton.textContent = "发送";
        }
        if (input) {
            input.disabled = !enabled;
            input.focus();
        }
    }

    function handleEvent(eventName, payload) {
        if (eventName === "meta") {
            conversationId = payload.conversationId;
            panel.dataset.conversationId = String(conversationId);
            var nextUrl = "/ai?conversationId=" + conversationId;
            if (window.location.pathname + window.location.search !== nextUrl) {
                window.history.replaceState({}, "", nextUrl);
            }
            addConversation(payload.title);
            return;
        }
        if (eventName === "sources") {
            var streamingBody = chatLog.querySelector(".message.assistant:last-child .message-body");
            if (streamingBody) {
                renderSources(streamingBody, payload.sources);
            }
            return;
        }
        if (eventName === "token") {
            var target = chatLog.querySelector(".message.assistant:last-child .message-text");
            if (target) {
                target.textContent += payload.text;
                scrollToBottom();
            }
            return;
        }
        if (eventName === "degraded") {
            removeEmptyAssistant();
            var degraded = document.createElement("div");
            degraded.className = "chat-degraded";
            degraded.textContent = payload.message;
            chatLog.appendChild(degraded);
            if (payload.sources && payload.sources.length) {
                var row = document.createElement("div");
                row.className = "message-sources";
                payload.sources.forEach(function (source) {
                    var link = document.createElement("a");
                    link.className = "message-source";
                    link.href = source.url;
                    link.textContent = source.title;
                    row.appendChild(link);
                });
                chatLog.appendChild(row);
            }
            scrollToBottom();
            finishStream();
            return;
        }
        if (eventName === "error") {
            removeEmptyAssistant();
            var error = document.createElement("div");
            error.className = "chat-degraded";
            error.textContent = payload.message || "AI 服务暂时不可用";
            chatLog.appendChild(error);
            scrollToBottom();
            finishStream();
            return;
        }
        if (eventName === "done") {
            conversationId = payload.conversationId;
            panel.dataset.conversationId = String(conversationId);
            finishStream();
            refreshMessages();
        }
    }

    function parseEvent(raw) {
        var eventName = "message";
        var dataLines = [];
        raw.split("\n").forEach(function (line) {
            if (line.indexOf("event:") === 0) {
                eventName = line.slice(6).trim();
            } else if (line.indexOf("data:") === 0) {
                dataLines.push(line.slice(5).replace(/^ /, ""));
            }
        });
        var payload = {};
        if (dataLines.length) {
            try {
                payload = JSON.parse(dataLines.join("\n"));
            } catch (error) {
                payload = {};
            }
        }
        return { name: eventName, payload: payload };
    }

    async function streamChat(message) {
        streaming = true;
        if (sendButton) {
            sendButton.disabled = true;
            sendButton.textContent = "生成中";
        }
        if (input) {
            input.disabled = true;
        }
        hideWelcome();
        makeMessage("user", message);
        makeMessage("assistant", "");

        var headers = { "Content-Type": "application/json", "Accept": "text/event-stream" };
        headers[csrfHeader] = csrfToken;

        try {
            var response = await fetch("/api/ai/chat", {
                method: "POST",
                headers: headers,
                body: JSON.stringify({ conversationId: conversationId, message: message })
            });
            if (!response.ok || !response.body) {
                throw new Error("请求失败");
            }
            var reader = response.body.getReader();
            var decoder = new TextDecoder("utf-8");
            var buffer = "";
            while (true) {
                var result = await reader.read();
                buffer += decoder.decode(result.value || new Uint8Array(), { stream: true }).replace(/\r\n/g, "\n");
                var boundary;
                while ((boundary = buffer.indexOf("\n\n")) >= 0) {
                    var raw = buffer.slice(0, boundary);
                    buffer = buffer.slice(boundary + 2);
                    if (raw.trim()) {
                        var event = parseEvent(raw);
                        handleEvent(event.name, event.payload);
                    }
                }
                if (result.done) {
                    break;
                }
            }
            var streamingBody = chatLog.querySelector(".message.assistant:last-child .message-body");
            if (streamingBody) {
                streamingBody.classList.remove("streaming-cursor");
            }
        } catch (error) {
            var box = document.createElement("div");
            box.className = "chat-degraded";
            box.textContent = "网络连接中断，请稍后重试";
            chatLog.appendChild(box);
            scrollToBottom();
        } finally {
            finishStream();
        }
    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        if (!enabled || streaming) {
            return;
        }
        var message = input.value.trim();
        if (message.length < 2) {
            return;
        }
        input.value = "";
        streamChat(message);
    });

    if (input) {
        input.addEventListener("keydown", function (event) {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                form.requestSubmit();
            }
        });
    }

    document.querySelectorAll(".prompt-chip").forEach(function (chip) {
        chip.addEventListener("click", function () {
            if (!enabled || streaming) {
                return;
            }
            input.value = chip.dataset.prompt || chip.textContent.trim();
            form.requestSubmit();
        });
    });

    if (!enabled) {
        input.disabled = true;
        sendButton.disabled = true;
    }
    scrollToBottom();
})();
