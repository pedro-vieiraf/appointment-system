const page = document.querySelector(".booking");
const date = document.querySelector("#date");
const slots = document.querySelector("#slots");
const options = document.querySelector("#slot-options");
const slotStatus = document.querySelector("#slot-status");
const bookingStatus = document.querySelector("#booking-status");
const chosenSlot = document.querySelector("#chosen-slot");
const reload = document.querySelector("#reload");
const form = document.querySelector("#booking-form");
const submit = document.querySelector("#submit");
const { linkId, timeZone, bookingEnabled, fixedOffset } = page.dataset;
const api = `/public/availability/${linkId}`;
const displayZone = fixedOffset === "" ? timeZone : "UTC";
const displayDate = (value) => new Date(new Date(value).getTime() + Number(fixedOffset) * 1000);
const time = new Intl.DateTimeFormat("pt-BR", {
  timeZone: displayZone, hour: "2-digit", minute: "2-digit"
});
const displayTime = (value) => time.format(displayDate(value));
const dateFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: displayZone, year: "numeric", month: "2-digit", day: "2-digit"
});
const today = Object.fromEntries(dateFormatter.formatToParts(displayDate(new Date()))
  .map(({ type, value }) => [type, value]));
date.min = `${today.year}-${today.month}-${today.day}`;
date.value = date.min;
form.hidden = bookingEnabled !== "true";
document.querySelector("#booking-disabled").hidden = !form.hidden;

let activeLoad;
let selected;
let submitting = false;

function selectSlot(slot) {
  selected = slot;
  submit.disabled = !selected || submitting;
  chosenSlot.textContent = slot
    ? `Horário escolhido: ${displayTime(slot.start)}–${displayTime(slot.end)}.`
    : "Escolha um horário na agenda.";
}

async function readMessage(response, fallback) {
  try {
    const result = await response.json();
    return result.message || fallback;
  } catch {
    return fallback;
  }
}

async function loadSlots() {
  activeLoad?.abort();
  const controller = new AbortController();
  activeLoad = controller;
  selectSlot(undefined);
  slots.hidden = true;
  reload.hidden = true;
  options.replaceChildren();

  if (!date.value || !date.validity.valid) {
    slotStatus.textContent = "Escolha uma data a partir de hoje.";
    return;
  }

  slotStatus.textContent = "Consultando horários…";
  try {
    const response = await fetch(`${api}/slots?date=${encodeURIComponent(date.value)}`,
      { signal: controller.signal });
    if (!response.ok) {
      throw new Error(await readMessage(response, "Não foi possível consultar a agenda."));
    }
    const available = await response.json();
    if (controller.signal.aborted) return;

    slotStatus.textContent = available.length ? "" : "Não há horários livres nessa data.";
    slots.hidden = available.length === 0;
    available.forEach((slot) => {
      const label = document.createElement("label");
      const input = document.createElement("input");
      input.type = "radio";
      input.name = "available-slot";
      input.disabled = form.hidden;
      const interval = `${displayTime(slot.start)}–${displayTime(slot.end)}`;
      input.addEventListener("change", () => selectSlot(slot));
      label.append(input, document.createTextNode(interval));
      options.append(label);
    });
  } catch (error) {
    if (controller.signal.aborted) return;
    slotStatus.textContent = error instanceof TypeError
      ? "Não foi possível acessar a agenda. Tente novamente." : error.message;
    reload.hidden = false;
  }
}

function showBookingMessage(message, isError) {
  bookingStatus.textContent = message;
  bookingStatus.dataset.error = isError;
  bookingStatus.focus();
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!selected || submitting || !form.reportValidity()) return;

  const data = new FormData(form);
  const payload = { name: data.get("name"), email: data.get("email"),
    date: date.value, start: selected.start };
  submitting = true;
  submit.disabled = true;
  bookingStatus.textContent = "Enviando solicitação…";
  try {
    const response = await fetch(`${api}/requests`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!response.ok) {
      let message = await readMessage(response, "Não foi possível enviar a solicitação.");
      const seconds = Number(response.headers.get("Retry-After"));
      if (response.status === 429 && seconds > 0) {
        const minutes = Math.ceil(seconds / 60);
        message = `Você enviou muitas solicitações. Tente novamente em ${minutes} min.`;
      }
      showBookingMessage(message, true);
      if (response.status === 409) await loadSlots();
      return;
    }
    showBookingMessage("Solicitação recebida. Aguarde a confirmação do profissional.", false);
    form.reset();
    await loadSlots();
  } catch {
    showBookingMessage("Não foi possível enviar a solicitação. Tente novamente.", true);
  } finally {
    submitting = false;
    submit.disabled = !selected;
  }
});

date.addEventListener("change", loadSlots);
reload.addEventListener("click", loadSlots);
loadSlots();
