/* ===== Fees & Installments (multi-year, all rows in DOM; toggle visibility) ===== */
var yearBudgets = new Map();           // current effective budgets (what UI uses)
var initialYearBudgets = new Map();    // snapshot from backend/template (for reference)
let hasCustomBudgets = false;
let budgetsSource = 'auto';
$(document).ready(function() {
  $('#course').trigger('change');
  $('#activeYear').trigger('change');
  
});
(function () {
	
  const totalFeesEl   = document.getElementById('totalFees');
  const discountEl    = document.getElementById('discountAmount');
  const actualFeesEl  = document.getElementById('actualFees');

  const courseYearsEl       = document.getElementById('courseYears');        // number input
  const activeYearSel       = document.getElementById('activeYear');        // year selector
  const activeYearBudgetEl  = document.getElementById('activeYearBudget');  // per-year budget input

  const countEl      = document.getElementById('installmentsCount');
  const tbody        = document.getElementById('installmentsBody');
  const sumEl        = document.getElementById('installmentsSum');
  const evenSplitBtn = document.getElementById('evenSplitBtn');

  // Flags/data from backend (set via Thymeleaf)
  const hasExistingInstallments =
    window.hasExistingInstallments === true ||
    window.hasExistingInstallments === 'true';

  const existingInstallments = Array.isArray(window.existingInstallments)
    ? window.existingInstallments
    : [];

  if (!totalFeesEl || !discountEl || !actualFeesEl ||
      !courseYearsEl || !activeYearSel || !activeYearBudgetEl ||
      !countEl || !tbody || !sumEl) return;

  // If MODES is defined elsewhere, use that. Otherwise you can uncomment default.
  // const MODES  = ['Cash', 'UPI', 'Card', 'BankTransfer', 'Cheque'];
  const STATUS = ['Un Paid','Paid'];

  /* ---------- utils ---------- */
  function toNumber(v) {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }
  function getCourseYears() {
    return Math.max(1, toNumber(courseYearsEl.value) || 1);
  }
  function getActiveYear() {
    return Math.min(Math.max(1, toNumber(activeYearSel.value) || 1), getCourseYears());
  }

  /* ---------- year budgets ---------- */
  // yearBudgets holds the target budget for each year (must sum to Actual Fees)

  function computeDefaultBudgets() {
    const years  = getCourseYears();
    const actual = toNumber(actualFeesEl.value);
    if (!years || !actual) return;

    yearBudgets.clear();

    const base = Math.floor(actual / years);
    let rem    = actual - base * years;

    for (let y = 1; y <= years; y++) {
      const add = rem > 0 ? 1 : 0;
      if (rem > 0) rem--;
      yearBudgets.set(y, base + add);
    }

    initialYearBudgets = new Map(yearBudgets); // snapshot
    budgetsSource = 'auto';
  }

  function getYearBudget(year = getActiveYear()) {
    // Lazily compute only if nothing exists AND we are in pure auto mode
    if ((!yearBudgets || yearBudgets.size === 0) &&
        !hasExistingInstallments &&
        budgetsSource === 'auto' &&
        !hasCustomBudgets) {
      computeDefaultBudgets();
    }
    return toNumber(yearBudgets.get(year));
  }

  function setYearBudget(year, value) {
    yearBudgets.set(year, Math.max(0, toNumber(value)));
  }

  /* ---------- DOM row helpers ---------- */
  function getAllRows() { return [...tbody.querySelectorAll('tr')]; }

  // Only rows whose year <= courseYears
  function getValidRows() {
    const maxYear = getCourseYears();
    return getAllRows().filter(tr => {
      const y = Number(tr.dataset.year || '1');
      return y >= 1 && y <= maxYear;
    });
  }
  // 🔹 All UNPAID amount inputs across valid years (<= courseYears)
  function getAllValidUnpaidAmountInputs() {
    const maxYear = getCourseYears();
    return getAllRows()
      .filter(tr => {
        const y = Number(tr.dataset.year || '1');
        return y >= 1 && y <= maxYear && !isPaidRow(tr);
      })
      .map(tr => tr.querySelector('input[data-field="amount"]'))
      .filter(Boolean);
  }

  // 🔥 Spread delta equally across all UNPAID installments (course years only)
  function adjustAllInstallmentsByDelta(delta) {
    const inputs = getAllValidUnpaidAmountInputs();
    const n = inputs.length;
    if (!n || !Number.isFinite(delta) || delta === 0) {
      recalcInstallmentsSum();
      return;
    }

    // We work in whole rupees
    let d = Math.round(delta);

    if (d > 0) {
      // ➕ Increase installments
      const base = Math.floor(d / n);
      let rem = d - base * n;

      inputs.forEach(inp => {
        let cur = toNumber(inp.value);
        let add = base;
        if (rem > 0) { add += 1; rem -= 1; }
        const nextVal = cur + add;
        inp.value = String(nextVal);
        inp.dataset.prev = String(nextVal);
      });

    } else if (d < 0) {
      // ➖ Decrease installments
      d = Math.abs(d);

      // Max we can subtract (don’t go below 0 on any installment)
      let totalCap = 0;
      inputs.forEach(inp => {
        totalCap += Math.max(0, toNumber(inp.value));
      });
      let need = Math.min(d, totalCap); // clamp

      if (need <= 0) {
        recalcInstallmentsSum();
        return;
      }

      const base = Math.floor(need / n);
      let rem = need - base * n;
      let remaining = need;

      inputs.forEach(inp => {
        if (remaining <= 0) return;
        let cur = toNumber(inp.value);

        let take = base;
        if (rem > 0) { take += 1; rem -= 1; }

        take = Math.min(take, cur);   // don’t go negative
        const nextVal = cur - take;
        remaining -= take;

        inp.value = String(nextVal);
        inp.dataset.prev = String(nextVal);
      });
    }

    recalcInstallmentsSum();
  }


  function clearAllInstallments() {
    getAllRows().forEach(tr => tr.remove());
    countEl.value = "0";

    if (sumEl) sumEl.value = 0;
    const yearSumEl = document.getElementById('yearInstallmentsSum');
    if (yearSumEl) yearSumEl.value = 0;

    recalcInstallmentsSum();
  }

  /* ---------- seed budgets from admission installments (backend) ---------- */
  function seedBudgetsFromAdmission(list) {
    yearBudgets.clear();
    list.forEach(inst => {
      const y = inst.yearNumber && inst.yearNumber > 0 ? inst.yearNumber : 1;
      const prev = yearBudgets.get(y) || 0;
      yearBudgets.set(y, prev + (inst.amount || 0));
    });

    initialYearBudgets = new Map(yearBudgets);
    budgetsSource = 'admission';
    hasCustomBudgets = false;
  }

  /* ---------- default fee structure load ---------- */
  async function loadDefaultFeeStructureForCourse(courseId) {
    if (!courseId) return;

    try {
      const resp = await fetch(`/course-fee?courseId=${courseId}`);
      if (!resp.ok) {
        console.error("Failed to fetch course fee template", await resp.text());
        return;
      }

      const data = await resp.json();
      const tmpl = data.feeTemplate;
      if (!tmpl) {
        console.log("No default fee template configured for course", courseId);
        clearAllInstallments();
        totalFeesEl.value = "";
        discountEl.value  = "";
        recalcActual({ reSplit: true });   // fallback to auto split
        return;
      }

      const installments = tmpl.installments || [];

      // 1) Set Total Fees & reset discount
      totalFeesEl.value = tmpl.totalAmount || 0;
      discountEl.value  = 0;
      // Do NOT re-split budgets here, just recompute Actual
      recalcActual({ reSplit: false });

      // 2) Clear existing installments in UI
      clearAllInstallments();

      // 3) Seed budgets from template (sum amount per year)
      yearBudgets.clear();
      installments.forEach(inst => {
        const y = inst.yearNumber && inst.yearNumber > 0 ? inst.yearNumber : 1;
        const prev = yearBudgets.get(y) || 0;
        yearBudgets.set(y, prev + (inst.amount || 0));
      });
      initialYearBudgets = new Map(yearBudgets);
      budgetsSource = 'template';
      hasCustomBudgets = false;

      // 4) Build year dropdown (DO NOT recompute budgets)
      ensureYearOptions({ skipBudgetRecalc: true });
      activeYearSel.value = "1";
      showYear(1);

      // 5) Add UI rows for each installment with correct year
      installments
        .sort((a, b) => (a.sequence || 0) - (b.sequence || 0))
        .forEach(function (inst) {
          const amt = inst.amount || 0;
          const y   = inst.yearNumber && inst.yearNumber > 0 ? inst.yearNumber : 1;
          activeYearSel.value = String(y);
          const year = getActiveYear();
          window.addInstallment({ amount: amt, year: year });
        });

      // back to year 1
      activeYearSel.value = "1";
      showYear(1);

      recalcInstallmentsSum();
    } catch (e) {
      console.error("Error loading default fee structure", e);
    }
  }

  /* ---------- existing admission – don't override with template ---------- */
  $("body").on("change", "#course", function () {
    let selectedCourseId = $(this).val();
    selectedCourseId = Number.parseInt(selectedCourseId);

    const course = courses.find(c => c.courseId === selectedCourseId);
    console.log("Selected course:", course);

    if (course && course.years) {
      $("#courseYears").val(course.years).trigger("change");
    }

    // Rebuild year options based on course years (no budget recalc here)
    ensureYearOptions({ skipBudgetRecalc: true });

    if (selectedCourseId) {
      if (hasExistingInstallments) {
        console.log("Admission has existing installments – skipping default fee template load.");
        // DO NOT call loadDefaultFeeStructureForCourse
      } else {
        loadDefaultFeeStructureForCourse(selectedCourseId);
      }
    } else {
      // No course selected – clear everything
      clearAllInstallments();
      totalFeesEl.value = "";
      discountEl.value  = "";
      recalcActual({ reSplit: true });
    }
  });

  /* ---------- sums ---------- */
  function getRows(year = getActiveYear()) {
    return [...tbody.querySelectorAll(`tr[data-year="${year}"]`)];
  }
  function getStatusCell(tr) { return tr.querySelector('select[data-field="status"]'); }
  function isPaidRow(tr) {
    const t = (getStatusCell(tr)?.value || '').trim().toLowerCase();
    return t === 'paid';
  }
  function unpaidRows(year = getActiveYear()) { return getRows(year).filter(tr => !isPaidRow(tr)); }

  function lockRowIfPaid(tr) {
    const locked = isPaidRow(tr);
    tr.querySelectorAll('input, select').forEach(el => {
      const f = el.getAttribute('data-field');
      if (['amount','date','mode','receipt'].includes(f)) el.disabled = locked;
    });
  }

  function paidAmountSum(year = getActiveYear()) {
    return getRows(year).reduce((sum, tr) => {
      if (!isPaidRow(tr)) return sum;
      return sum + toNumber(tr.querySelector('input[data-field="amount"]')?.value);
    }, 0);
  }

  function recalcInstallmentsSum() {
    const year = getActiveYear();

    const sumEl        = document.getElementById('installmentsSum');
    const yearSumEl    = document.getElementById('yearInstallmentsSum');
    const overallEl    = document.getElementById('overallInstallmentsSum');
    const actualInput  = document.getElementById('actualFees');

    // YEAR sum: active year's rows
    let yearSum = 0;
    getRows(year).forEach(tr => {
      const amtInput = tr.querySelector('input[data-field="amount"]');
      if (!amtInput) return;
      yearSum += toNumber(amtInput.value);
    });

    // OVERALL sum: only rows within courseYears
    let overall = 0;
    getValidRows().forEach(tr => {
      const amtInput = tr.querySelector('input[data-field="amount"]');
      if (!amtInput) return;
      overall += toNumber(amtInput.value);
    });

    if (yearSumEl) {
      yearSumEl.value = yearSum;
      const yearBudget = getYearBudget(year);
      yearSumEl.classList.toggle(
        'is-invalid',
        yearBudget > 0 && yearSum !== yearBudget
      );
    }

    if (overallEl) overallEl.value = overall;
    if (sumEl)     sumEl.value     = overall;

    const actual = toNumber(actualInput?.value);
    const mismatch = !!actual && overall !== actual;

    if (sumEl)     sumEl.classList.toggle('is-invalid', mismatch);
    if (overallEl) overallEl.classList.toggle('is-invalid', mismatch);
  }

  /* ---------- Actual fees + budgets ---------- */
  function recalcActual(opts = {}) {
    // opts.reSplit:
    //  - true  → force equal-split recompute (only when we WANT auto mode)
    //  - false → keep existing budgets as-is
    const reSplit = opts.reSplit === true;

    const total  = toNumber(totalFeesEl.value);
    const disc   = Math.min(toNumber(discountEl.value), total);
    const actual = Math.max(total - disc, 0);
    actualFeesEl.value = actual;

    // Only recompute budgets if we explicitly want auto mode AND we are not using template/admission/custom budgets
    if (reSplit &&
        budgetsSource === 'auto' &&
        !hasExistingInstallments &&
        !hasCustomBudgets) {
      computeDefaultBudgets();
    }

    refreshActiveYearBudgetInput();
    recalcInstallmentsSum();
  }

  /* ---------- UI builders ---------- */
  function buildModeSelect(nameId) {
    const sel = document.createElement('select');
    sel.className = 'form-select';
    sel.setAttribute('data-field', 'mode');
    sel.id = nameId;
    const optinitial = document.createElement('option');
    optinitial.value = ""; optinitial.textContent = "Select";
    sel.appendChild(optinitial);
    MODES.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m; opt.textContent = m;
      sel.appendChild(opt);
    });
    return sel;
  }

  function buildStatusSelect(nameId) {
    const sel = document.createElement('select');
    sel.className = 'form-select';
    sel.setAttribute('data-field', 'status');
    sel.id = nameId;
    const optinitial = document.createElement('option');
    optinitial.value = ""; optinitial.textContent = "Select";
    sel.appendChild(optinitial);
    STATUS.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m; opt.textContent = m;
      sel.appendChild(opt);
    });
    return sel;
  }

  /* ---------- numbering ---------- */
  function renumberSrNos(year = getActiveYear()) {
    getRows(year).forEach((tr, idx) => {
      const tdSr = tr.querySelector('td:first-child');
      if (tdSr) tdSr.textContent = String(idx + 1);
    });
  }

  /* ---------- build row (tagged with data-year) ---------- */
  function buildRow(rowIndexInYear, year, statusText = 'Un Paid') {
    const tr = document.createElement('tr');
    tr.dataset.year = String(year);

    // Sr No
    const tdSr = document.createElement('td');
    tdSr.textContent = String(rowIndexInYear + 1);
    tr.appendChild(tdSr);

    // Amount
    const tdAmt = document.createElement('td');
    const amt = document.createElement('input');
    amt.type = 'number'; amt.min = '0'; amt.step = '1';
    amt.className = 'form-control'; amt.placeholder = '0';
    amt.setAttribute('data-field', 'amount');
    amt.id = `inst_${year}_${rowIndexInYear}_amount`;
    amt.addEventListener('input', recalcInstallmentsSum);
    attachAmountListeners(amt);
    tdAmt.appendChild(amt);
    tr.appendChild(tdAmt);

    // Date
    const tdDate = document.createElement('td');
    const date = document.createElement('input');
    date.type = 'date'; date.className = 'form-control';
    date.setAttribute('data-field', 'date');
    date.id = `inst_${year}_${rowIndexInYear}_date`;
    tdDate.appendChild(date);
    tr.appendChild(tdDate);

    // Mode
    const tdMode = document.createElement('td');
    const modeSel = buildModeSelect(`inst_${year}_${rowIndexInYear}_mode`);
    tdMode.appendChild(modeSel);
    tr.appendChild(tdMode);

    // Receipt (file)
    const tdFile = document.createElement('td');
    const file = document.createElement('input');
    file.type = 'file'; file.className = 'form-control';
    file.accept = '.pdf,.jpg,.jpeg,.png';
    file.setAttribute('data-field', 'receipt');
    file.id = `inst_${year}_${rowIndexInYear}_file`;
    tdFile.appendChild(file);
    tr.appendChild(tdFile);

    // Received By
    const tdReceivedBy = document.createElement('td');
    const receivedBy = document.createElement('input');
    receivedBy.type = 'text';
    receivedBy.className = 'form-control';
    receivedBy.setAttribute('data-field', 'receivedBy');
    receivedBy.id = `inst_${year}_${rowIndexInYear}_receivedBy`;
    tdReceivedBy.appendChild(receivedBy);
    tr.appendChild(tdReceivedBy);

    // Status
    const tdStatus = document.createElement('td');
    const statusSel = buildStatusSelect(`inst_${year}_${rowIndexInYear}_status`);
    statusSel.value = statusText;
    tdStatus.appendChild(statusSel);
    tr.appendChild(tdStatus);

    lockRowIfPaid(tr);
    return tr;
  }

  /* ---------- show/hide rows per active year (keep all in DOM) ---------- */
  function showYear(year) {
    getAllRows().forEach(tr => {
      tr.hidden = (tr.dataset.year !== String(year));
    });
    countEl.value = String(getRows(year).length);
    refreshActiveYearBudgetInput();
    renumberSrNos(year);
    recalcInstallmentsSum();
  }

  /* ---------- add/remove rows ONLY in active year ---------- */
  function addUnpaidRows(countToAdd, year = getActiveYear()) {
    const startIdx = getRows(year).length;
    for (let k = 0; k < countToAdd; k++) {
      tbody.appendChild(buildRow(startIdx + k, year, 'Un Paid'));
    }
    renumberSrNos(year);
  }

  function removeUnpaidRowsFromBottom(countToRemove, year = getActiveYear()) {
    let remaining = countToRemove;
    const rows = getRows(year).reverse();
    for (const tr of rows) {
      if (remaining <= 0) break;
      if (!isPaidRow(tr)) {
        tr.remove();
        remaining--;
      }
    }
    renumberSrNos(year);
    return remaining;
  }

  /* ---------- amount redistribution (year-scoped) ---------- */
  function getAmountInputs(year = getActiveYear()) {
    return unpaidRows(year)
      .map(tr => tr.querySelector('input[data-field="amount"]'))
      .filter(Boolean);
  }

  function redistributeDeltaBelow(startIdx, delta, year = getActiveYear()) {
    const inputs = getAmountInputs(year);
    const below  = inputs.slice(startIdx + 1);
    const m = below.length;
    if (m === 0 || !Number.isFinite(delta) || delta === 0) { recalcInstallmentsSum(); return; }

    let remaining = Math.trunc(Math.round(delta));
    const sign = remaining > 0 ? 1 : -1;
    const abs  = Math.abs(remaining);
    const base = Math.floor(abs / m);
    let rem    = abs - base * m;

    for (let i = 0; i < m; i++) {
      const inp = below[i];
      const cur = toNumber(inp.value);
      let adj = base + (rem > 0 ? 1 : 0);
      if (rem > 0) rem--;

      if (sign > 0) { // take from below
        const nextVal = Math.max(cur - adj, 0);
        const actuallySubtracted = cur - nextVal;
        remaining -= actuallySubtracted;
        inp.value = nextVal;
      } else {        // give to below
        inp.value = cur + adj;
      }
    }

    recalcInstallmentsSum();
  }

  function redistributeDeltaAbove(startIdx, delta, year = getActiveYear()) {
    const inputs = getAmountInputs(year);
    const above  = inputs.slice(0, startIdx);
    const m = above.length;
    if (m === 0 || !Number.isFinite(delta) || delta === 0) { recalcInstallmentsSum(); return; }

    let remaining = Math.trunc(Math.round(delta));
    const sign = remaining > 0 ? 1 : -1;
    const abs  = Math.abs(remaining);
    const base = Math.floor(abs / m);
    let rem    = abs - base * m;

    for (let i = m - 1; i >= 0; i--) {
      const inp = above[i];
      const cur = toNumber(inp.value);
      let adj = base + (rem > 0 ? 1 : 0);
      if (rem > 0) rem--;

      if (sign > 0) { // take from above
        const nextVal = Math.max(cur - adj, 0);
        const actuallySubtracted = cur - nextVal;
        remaining -= actuallySubtracted;
        inp.value = nextVal;
      } else {        // give to above
        inp.value = cur + adj;
      }
    }

    recalcInstallmentsSum();
  }

  function attachAmountListeners(amountInput) {
    amountInput.addEventListener('focus', () => {
      amountInput.dataset.prev = String(toNumber(amountInput.value));
    });

    amountInput.addEventListener('input', () => {
      const prev = toNumber(amountInput.dataset.prev ?? amountInput.value);
      const now  = toNumber(amountInput.value);
      const delta = now - prev;
      amountInput.dataset.prev = String(now);

      const year = Number(amountInput.closest('tr')?.dataset.year || getActiveYear());
      const inputs = getAmountInputs(year);
      const startIdx = inputs.indexOf(amountInput);
      if (startIdx === -1) { recalcInstallmentsSum(); return; }

      // If last unpaid row edited → distribute to ABOVE; else to BELOW
      if (startIdx === inputs.length - 1) {
        redistributeDeltaAbove(startIdx, delta, year);
      } else {
        redistributeDeltaBelow(startIdx, delta, year);
      }
    });
  }

  /* ---------- even split (UNPAID in active year; uses yearBudget) ---------- */
  function equalSplitUnpaidAcrossCurrent() {
    const year = getActiveYear();
    const rowsUnpaid = unpaidRows(year);
    const n = rowsUnpaid.length;
    if (!n) { recalcInstallmentsSum(); return; }

    const targetBudget = getYearBudget(year);
    const paidSumY = paidAmountSum(year);
    let unpaidTotal = Math.max(targetBudget - paidSumY, 0);

    const base = Math.floor(unpaidTotal / n);
    let remainder = unpaidTotal - base * n;

    rowsUnpaid.forEach(tr => {
      const inp = tr.querySelector('input[data-field="amount"]');
      if (!inp) return;
      let val = base;
      if (remainder > 0) { val += 1; remainder -= 1; }
      inp.value = val;
      inp.dataset.prev = String(val);
    });

    recalcInstallmentsSum();
  }

  /* ---------- change number of rows ONLY in active year ---------- */
  function adjustRowsToCount(desiredCount) {
    const year = getActiveYear();
    const current = getRows(year).length;
    const paidCnt = getRows(year).filter(isPaidRow).length;
    const target  = Math.max(toNumber(desiredCount), paidCnt);

    if (target > current) {
      addUnpaidRows(target - current, year);
      equalSplitUnpaidAcrossCurrent();
    } else if (target < current) {
      const leftover = removeUnpaidRowsFromBottom(current - target, year);
      if (leftover > 0) {
        countEl.value = String(getRows(year).length); // cannot go below paid
      }
      equalSplitUnpaidAcrossCurrent();
    }

    showYear(year);
  }
  window.adjustRowsToCount = adjustRowsToCount;

  /* ---------- extract all rows (for submit) ---------- */
  window.getInstallmentsData = function () {
    const rows = getAllRows();
    return rows.map((tr, idx) => {
      const amount = toNumber(tr.querySelector('input[data-field="amount"]')?.value);
      const date   = tr.querySelector('input[data-field="date"]')?.value || null;
      const mode   = tr.querySelector('select[data-field="mode"]')?.value || null;
      const file   = tr.querySelector('input[data-field="receipt"]')?.files?.[0] || null;
      const receivedBy = tr.querySelector('input[data-field="receivedBy"]')?.value || null;
      const status = tr.querySelector('select[data-field="status"]')?.value || 'Un Paid';
      const year   = Number(tr.dataset.year || getActiveYear());
      return { srNo: idx + 1, amount, date, mode, file, receivedBy, status, year };
    });
  };

  /* ---------- public add APIs (respect active year) ---------- */
  window.addInstallment = function addInstallment(opts = {}) {
    const year = opts.year || getActiveYear();
    const i = getRows(year).length;
    const tr = buildRow(i, year, (opts.status || 'Un Paid').trim());
    tbody.appendChild(tr);

    const amtInp  = tr.querySelector('input[data-field="amount"]');
    const dateInp = tr.querySelector('input[data-field="date"]');
    const modeSel = tr.querySelector('select[data-field="mode"]');

    if (typeof opts.amount === 'number' && Number.isFinite(opts.amount)) {
      amtInp.value = String(opts.amount);
      amtInp.dataset.prev = String(opts.amount);
    }
    if (typeof opts.date === 'string' && opts.date) {
      dateInp.value = opts.date;
    }
    if (typeof opts.mode === 'string' && modeSel) {
      const opt = [...modeSel.options].find(o => o.value === opts.mode);
      if (opt) modeSel.value = opts.mode;
    }

    lockRowIfPaid(tr);
    countEl.value = String(getRows(year).length);
    showYear(year);
    return tr;
  };

  window.addInstallments = function addInstallments(count, opts = {}) {
    const year = getActiveYear();
    const n = Math.max(0, Number(count) || 0);
    if (!n) return;
    for (let k = 0; k < n; k++) window.addInstallment({ ...opts, resplit: false });
    equalSplitUnpaidAcrossCurrent();
    showYear(year);
  };

  /* ---------- YEAR BUDGET UI + CASCADE ---------- */

  function refreshActiveYearBudgetInput() {
    const y = getActiveYear();
    activeYearBudgetEl.value = String(getYearBudget(y));
  }

  function maxSubtractableForYear(y) {
    const currentBudget = getYearBudget(y);
    const paidY = paidAmountSum(y);
    return Math.max(0, currentBudget - paidY);
  }
  // 🔥 Spread a delta in Actual Fees equally across all yearly budgets
  function adjustAllYearBudgetsByDelta(delta) {
    const years = getCourseYears();
    if (years <= 0) return;

    // Make sure we have some budgets to work with
    if (!yearBudgets || yearBudgets.size === 0) {
      computeDefaultBudgets();
    }

    // Increasing total budget → just add equally (no min constraint)
    if (delta > 0) {
      const abs = Math.abs(delta);
      const base = Math.floor(abs / years);
      let rem = abs - base * years;

      for (let y = 1; y <= years; y++) {
        let add = base;
        if (rem > 0) { add += 1; rem -= 1; }
        setYearBudget(y, getYearBudget(y) + add);
      }
    }

    // Decreasing total budget → subtract equally, but never go below already paid
    if (delta < 0) {
      let need = Math.abs(delta);

      // Compute how much we *can* subtract in total (respecting paid amounts)
      const caps = [];
      let totalCap = 0;
      for (let y = 1; y <= years; y++) {
        const cap = maxSubtractableForYear(y); // currentBudget - paidY (>=0)
        caps.push({ year: y, cap });
        totalCap += cap;
      }

      if (totalCap <= 0) {
        // Nothing can be reduced without violating paid installments – abort
        return;
      }

      if (need > totalCap) {
        need = totalCap; // can't subtract more than possible
      }

      // Subtract in a round-robin way to keep it as equal as possible
      let guard = 10000; // safety guard
      while (need > 0 && guard-- > 0) {
        let progress = false;
        for (let y = 1; y <= years && need > 0; y++) {
          const cap = maxSubtractableForYear(y);
          if (cap <= 0) continue;

          setYearBudget(y, getYearBudget(y) - 1);
          need -= 1;
          progress = true;
        }
        if (!progress) break;
      }
    }

    // After discount change, budgets are effectively "custom"
    hasCustomBudgets = true;
    budgetsSource = 'custom';
  }

  function cascadeDeltaFromYear(startYear, delta) {
    let years = getCourseYears();
    let remaining = delta;

    function subtractFromYear(y, need) {
      const cap = maxSubtractableForYear(y);
      const take = Math.min(cap, need);
      if (take > 0) setYearBudget(y, getYearBudget(y) - take);
      return need - take;
    }

    if (remaining > 0) {
      if (startYear < years) {
        for (let y = startYear + 1; y <= years && remaining > 0; y++) {
          remaining = subtractFromYear(y, remaining);
        }
      }
      for (let y = startYear - 1; y >= 1 && remaining > 0; y--) {
        remaining = subtractFromYear(y, remaining);
      }
    } else if (remaining < 0) {
      const addVal = -remaining;
      if (startYear < years) {
        setYearBudget(startYear + 1, getYearBudget(startYear + 1) + addVal);
        remaining = 0;
      } else if (startYear > 1) {
        setYearBudget(startYear - 1, getYearBudget(startYear - 1) + addVal);
        remaining = 0;
      }
    }
  }

  function handleActiveYearBudgetEdit() {
    const y = getActiveYear();
    const oldBudget = getYearBudget(y);
    let newBudget   = toNumber(activeYearBudgetEl.value);

    const minAllowed = paidAmountSum(y);
    if (newBudget < minAllowed) {
      newBudget = minAllowed;
      activeYearBudgetEl.value = String(newBudget);
    }

    const delta = newBudget - oldBudget;
    if (delta === 0) return;

    // from here onwards, don’t auto override
    hasCustomBudgets = true;
    budgetsSource    = 'custom';

    setYearBudget(y, newBudget);
    cascadeDeltaFromYear(y, delta);

    equalSplitUnpaidAcrossCurrent();
    recalcInstallmentsSum();
  }

  /* ---------- year UI helpers ---------- */
  function ensureYearOptions(options = {}) {
    const { skipBudgetRecalc = false } = options;

    const years = getCourseYears();
    let cur = getActiveYear();

    activeYearSel.innerHTML = '';
    for (let y = 1; y <= years; y++) {
      const opt = document.createElement('option');
      opt.value = String(y);
      opt.textContent = `Year ${y}`;
      activeYearSel.appendChild(opt);
    }

    activeYearSel.value = String(Math.min(cur, years));

    // IMPORTANT:
    // Only recompute budgets for *fresh, auto* mode admissions
    if (!skipBudgetRecalc &&
        !hasExistingInstallments &&
        budgetsSource === 'auto' &&
        !hasCustomBudgets) {
      computeDefaultBudgets();
    }
  }

  /* ---------- wire events ---------- */
  totalFeesEl.addEventListener('input', () =>
    recalcActual({ reSplit: budgetsSource === 'auto' })
  );
  discountEl.addEventListener('input', handleDiscountChange);

  discountEl.addEventListener('input', handleDiscountChange);

  function handleDiscountChange() {
    // 1️⃣ Old Actual before discount change
    const oldActual = toNumber(actualFeesEl.value);

    // 2️⃣ Recalculate Actual (NO resplit of budgets here)
    recalcActual({ reSplit: false });

    // 3️⃣ Delta between new Actual and old Actual
    const newActual = toNumber(actualFeesEl.value);
    const deltaRaw = newActual - oldActual;  // may be + or -
    const delta = Math.round(deltaRaw);      // work in whole rupees

    if (delta === 0) {
      refreshActiveYearBudgetInput();
      recalcInstallmentsSum();
      return;
    }

    // 4️⃣ Adjust yearly budgets by this delta
    adjustAllYearBudgetsByDelta(delta);

    // 5️⃣ Adjust all installments by this delta
    adjustAllInstallmentsByDelta(delta);

    // 6️⃣ Refresh UI
    refreshActiveYearBudgetInput();
    recalcInstallmentsSum();
  }



  countEl.addEventListener('change', e => adjustRowsToCount(e.target.value));
  evenSplitBtn?.addEventListener('click', equalSplitUnpaidAcrossCurrent);

  courseYearsEl.addEventListener('change', () => ensureYearOptions());
  activeYearSel.addEventListener('change', () => showYear(getActiveYear()));

  ['change','blur','input'].forEach(evt => {
    activeYearBudgetEl.addEventListener(evt, handleActiveYearBudgetEdit);
  });

  /* ---------- initial pass ---------- */
  getAllRows().forEach(tr => {
    if (!tr.dataset.year) tr.dataset.year = '1';
    lockRowIfPaid(tr);
    const amt = tr.querySelector('input[data-field="amount"]');
    if (amt && amt.dataset.prev == null) {
      amt.dataset.prev = String(toNumber(amt.value));
    }
  });

  if (hasExistingInstallments && existingInstallments.length > 0) {
    // Seed budgets from backend installments (admission case)
    seedBudgetsFromAdmission(existingInstallments);
    ensureYearOptions({ skipBudgetRecalc: true }); // don’t touch budgets
    recalcInstallmentsSum();
  } else {
    // New admission – auto budgets from total + years
    recalcActual({ reSplit: true });
    ensureYearOptions();
  }

  showYear(getActiveYear());
  refreshActiveYearBudgetInput();
})();
