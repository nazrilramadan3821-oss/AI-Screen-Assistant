// NIKO AI - Auto Answer Engine
// Tambahan fitur, tidak menghapus kode lama.

(() => {
  if (window.NIKO_AUTO_ANSWER) return;

  const NIKO = {
    questions: [],
    answers: [],

    // Membaca soal dan pilihan jawaban
    scan() {
      const groups = [
        ...document.querySelectorAll(
          '[role="radiogroup"], fieldset, .question, [data-question]'
        )
      ];

      const results = groups.map((group, index) => {
        const options = [
          ...group.querySelectorAll(
            'input[type="radio"], [role="radio"]'
          )
        ];

        const labels = options.map((option, i) => {
          const label =
            option.labels?.[0]?.innerText ||
            option.getAttribute('aria-label') ||
            option.parentElement?.innerText || '';

          return {
            index: i,
            text: label.trim(),
            element: option
          };
        }).filter(x => x.text);

        return {
          index,
          question: group.innerText.trim(),
          options: labels
        };
      }).filter(q => q.question && q.options.length);

      this.questions = results;
      console.log('NIKO: Soal ditemukan', results);
      return results;
    },

    // Meminta AI menganalisis soal
    async solve(apiUrl) {
      const questions = this.scan();

      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          questions: questions.map(q => ({
            question: q.question,
            options: q.options.map(o => o.text)
          }))
        })
      });

      if (!response.ok) {
        throw new Error('Gagal mendapatkan jawaban AI');
      }

      const data = await response.json();
      this.answers = data.answers || [];
      return this.answers;
    },

    // Mencocokkan jawaban AI dengan pilihan
    match(answer, options) {
      const normalize = s =>
        String(s).toLowerCase().trim()
          .replace(/^[a-e][.)]\s*/, '')
          .replace(/\s+/g, ' ');

      const wanted = normalize(answer);

      return options.find(o =>
        normalize(o.text) === wanted
      ) || null;
    },

    // Mengisi pilihan setelah diperiksa pengguna
    fill(answers = this.answers) {
      let filled = 0;

      answers.forEach((answer, index) => {
        const question = this.questions[index];
        if (!question) return;

        const selected = this.match(
          answer.answer,
          question.options
        );

        if (!selected) return;

        const element = selected.element;

        if (element.disabled) return;

        if (element.type === 'radio') {
          element.click();
        } else if (element.getAttribute('role') === 'radio') {
          element.click();
        }

        filled++;
      });

      console.log(`NIKO: ${filled} jawaban diisi`);
      return filled;
    }
  };

  window.NIKO_AUTO_ANSWER = NIKO;
  console.log('NIKO Auto Answer siap!');
})();
