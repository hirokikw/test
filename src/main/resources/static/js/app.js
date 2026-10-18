// 画面の操作は fetch で REST API を呼ぶ。入力値は textContent で表示し、innerHTML には入れない。
const registerForm = document.getElementById("register-form");
const nameInput = document.getElementById("name");
const registerButton = document.getElementById("register-button");
const batchButton = document.getElementById("batch-button");
const reloadButton = document.getElementById("reload-button");
const feedback = document.getElementById("feedback");
const greetingList = document.getElementById("greeting-list");

const dateTimeFormat = new Intl.DateTimeFormat("ja-JP", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
});

registerForm.addEventListener("submit", (event) => {
    event.preventDefault();
    withBusy(registerButton, registerGreeting);
});

batchButton.addEventListener("click", () => {
    withBusy(batchButton, runBatch);
});

reloadButton.addEventListener("click", () => {
    withBusy(reloadButton, async () => {
        const ok = await fetchAndRender();
        if (ok) {
            showFeedback("一覧を更新しました。", false);
        }
    });
});

withBusy(reloadButton, fetchAndRender);

async function registerGreeting() {
    try {
        const response = await fetch("/api/greetings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: nameInput.value })
        });
        if (!response.ok) {
            showFeedback(await readErrorMessage(response), true);
            return;
        }
        nameInput.value = "";
        showFeedback("登録しました。", false);
        await fetchAndRender();
    } catch (error) {
        showFeedback("通信に失敗しました。", true);
    }
}

async function runBatch() {
    try {
        const response = await fetch("/api/batch/greetings", { method: "POST" });
        if (!response.ok) {
            showFeedback(await readErrorMessage(response), true);
            return;
        }
        const body = await response.json();
        const count = Number.isInteger(body.processedCount) ? body.processedCount : 0;
        showFeedback("バッチが完了しました。処理件数: " + count, false);
        await fetchAndRender();
    } catch (error) {
        showFeedback("通信に失敗しました。", true);
    }
}

async function fetchAndRender() {
    try {
        const response = await fetch("/api/greetings");
        if (!response.ok) {
            showFeedback(await readErrorMessage(response), true);
            return false;
        }
        renderGreetings(await response.json());
        return true;
    } catch (error) {
        showFeedback("通信に失敗しました。", true);
        return false;
    }
}

function renderGreetings(greetings) {
    greetingList.replaceChildren();
    if (!Array.isArray(greetings) || greetings.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 6;
        cell.className = "empty";
        cell.textContent = "登録データはありません。";
        row.append(cell);
        greetingList.append(row);
        return;
    }
    for (const greeting of greetings) {
        const row = document.createElement("tr");
        row.append(
            textCell(greeting.id),
            textCell(greeting.name),
            textCell(statusLabel(greeting.status)),
            textCell(greeting.message ? greeting.message : "未生成"),
            textCell(formatDateTime(greeting.createdAt)),
            textCell(greeting.processedAt ? formatDateTime(greeting.processedAt) : "—")
        );
        greetingList.append(row);
    }
}

function textCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value == null ? "" : String(value);
    return cell;
}

function statusLabel(status) {
    if (status === "PENDING") {
        return "未処理";
    }
    if (status === "COMPLETED") {
        return "処理済み";
    }
    return status == null ? "" : String(status);
}

function formatDateTime(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    return dateTimeFormat.format(date);
}

function showFeedback(message, isError) {
    feedback.textContent = message;
    feedback.className = isError ? "feedback error" : "feedback success";
}

async function readErrorMessage(response) {
    try {
        const body = await response.json();
        if (body && typeof body.message === "string" && body.message) {
            return body.message;
        }
    } catch (error) {
        // messageが取れないときは汎用文にする。レスポンス本文は画面に出さない
    }
    return "処理に失敗しました。";
}

async function withBusy(button, action) {
    button.disabled = true;
    try {
        await action();
    } finally {
        button.disabled = false;
    }
}
