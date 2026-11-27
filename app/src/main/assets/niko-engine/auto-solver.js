/**
 * NIKO AI AUTO SOLVER V2
 * Question scanning, AI solving and answer matching.
 * Compatible with browser-based NIKO projects.
 */

(function () {
  "use strict";

  if (window.NIKO_SOLVER) return;

  const NIKO = {
    questions: [],
    answers: [],
    apiUrl: "https://niko-gemini.nazril-niko.workers.dev/api/solve",
    token: "",
    scanning: false,

    log(...args) {
      console.log("[NIKO SOLVER]", ...args);
    },

    // 1. Read visible questions
    scanScreen() {
      const selectors = [
        '[role="radiogroup"]',
        'fieldset',
        '.question',
        '[data-question]',
        '.Qr7Oae'
      ];

      const elements = [
        ...new Set(
          selectors.flatMap(s =>
            [...document.querySelectorAll(s)]
          )
        )
      ];

      const questions = elements.map((el, index) => {
        const options = [
          ...el.querySelectorAll(
            'input[type="radio"], [role="radio"]'
          )
        ].map((item, i) => ({
          index: i,
          text:
            item.labels?.[0]?.innerText ||
            item.getAttribute("aria-label") ||
            item.parentElement?.innerText ||
            "",
          element: item
        })).filter(x => x.text.trim());

        return {
          id: index,
          text: el.innerText.trim(),
          options
        };
      }).filter(q => q.text && q.options.length);

      this.questions = questions;
      this.log("Questions detected:", questions.length);

      return questions.map(q => ({
        id: q.id,
        question: q.text,
        options: q.options.map(o => o.text)
      }));
    },

    // 2. Send questions to AI backend
    async solve() {
      const questions = this.scanScreen();

      if (!questions.length) {
        throw new Error("Tidak ada soal yang ditemukan.");
      }

      const response = await fetch(this.apiUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-app-token": this.token
        },
        body: JSON.stringify({ questions })
      });

      if (!response.ok) {
        throw new Error("AI gagal (" + response.status + ")");
      }

      const result = await response.json();

      this.answers = result.answers || [];

      this.log("AI answers received:", this.answers);

      return this.answers;
    },

    // 3. Match AI answer with available options
    matchAnswer(answer, options) {
      const normalize = value =>
        String(value || "")
          .toLowerCase()
          .trim()
          .replace(/^[a-e][.)]\s*/, "")
          .replace(/\s+/g, " ");

      const target = normalize(answer);

      return options.find(option =>
        normalize(option.text) === target
      ) || null;
    },

    // 4. Preview matched answers before filling
    preview() {
      return this.answers.map((answer, index) => {
        const question = this.questions[index];

        if (!question) {
          return { index, matched: false };
        }

        const match = this.matchAnswer(
          answer.answer,
          question.options
        );

        return {
          index,
          question: question.text,
          answer: answer.answer,
          matched: Boolean(match),
          selectedText: match?.text || null
        };
      });
    },

    // 5. Fill matched answers after user approval
    fillAnswers() {
      const preview = this.preview();

      let filled = 0;

      preview.forEach((item, index) => {
        if (!item.matched) return;

        const question = this.questions[index];

        const selected = this.matchAnswer(
          this.answers[index].answer,
          question.options
        );

        if (!selected || selected.element.disabled) return;

        selected.element.click();
        filled++;
      });

      this.log("Filled:", filled);

      return filled;
    },

    // 6. Reset current session
    reset() {
      this.questions = [];
      this.answers = [];
      this.log("Session reset");
    }
  };

  window.NIKO_SOLVER = NIKO;

  NIKO.log("NIKO AUTO SOLVER V2 READY");
})();
