/* ===== Fees & Installments (multi-year, all rows in DOM; toggle visibility) ===== */
var yearBudgets = new Map();           // current effective budgets (what UI uses)
var initialYearBudgets = new Map();    // snapshot from backend/template (for reference)
let hasCustomBudgets = false;
let budgetsSource = 'auto';
$(document).ready(function() {
  $('#course').trigger('change');
  
  
  
});

jQuery(window).on("load", function () {
		$('#activeYear').trigger('change');

		setTimeout(function () {
		       $('#activeYear').trigger('change');
		    }, 5000);
	});

(function () {
	
	$(document).ready(function() {
	  
	  
	  $("body").on("click",".installment_delete_btn",function(){
		console.log(this.closest('tr'));
		handleDeleteInstallmentRow(this.closest('tr'),true,this.id)
	  })
	  
	});

  const totalFeesEl   = document.getElementById('totalFees');
  const discountEl    = document.getElementById('discountAmount');
  const actualFeesEl  = document.getElementById('actualFees');

  const courseYearsEl       = document.getElementById('courseYears');       // number input
  const activeYearSel       = document.getElementById('activeYear');       // year selector
  const activeYearBudgetEl  = document.getElementById('activeYearBudget'); // per-year budget input

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

  // MODES should be defined globally elsewhere
  const STATUS = ['Un Paid', 'Paid'];

  // 🔹 NEW: backend can set window.canDeleteInstallment = false to disable delete
  const canDeleteInstallment = window.canDeleteInstallment !== false;

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

  /* ---------- sums & status helpers ---------- */
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
      if (['amount', 'date', 'mode', 'receipt'].includes(f)) el.disabled = locked;
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

    // helpful return (optional, for debugging)
    return { yearSum, overall, actual };
  }

  // 🔓 Expose it so we can trigger from HTML after everything loads
  window.recalcInstallmentsSum = recalcInstallmentsSum;


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

  // 🔹 NEW: handler for delete
  function handleDeleteInstallmentRow(tr,isView,instId) {
    const year = Number(tr.dataset.year || getActiveYear());
    if (isPaidRow(tr)) {
      alert('Paid installment cannot be deleted.');
      return;
    }

    tr.remove();
    renumberSrNos(year);

    // update count for active year
    if (year === getActiveYear()) {
      countEl.value = String(getRows(year).length);
    }

    // re-split remaining unpaid installments in that year
    equalSplitUnpaidForYear(year);
    recalcInstallmentsSum();
	
	if(isView){
		apiCallForInstallmentDeletion(instId)
	}
	
  }

  function apiCallForInstallmentDeletion(instId){
	instId = instId.split("_")[1]
	$.ajax({
	  url: "/api/installments/" + instId + "/delete?deleteFilesAlso="+true,
	  type: "DELETE",
	  success: function(res){
	    alert(res.message || "Deleted");
	    // remove row
	    //$(btn).closest("tr").remove();
		$("#submit_admission_form").click()
	  },
	  error: function(xhr){
	    alert("Delete failed: " + (xhr.responseJSON?.message || xhr.responseText));
		location.reload()
	  }
	});
  }
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
	
	
	// TXNID
   const tdTxnRef = document.createElement('td');
   const txnRef = document.createElement('input');
   txnRef.type = 'text'; txnRef.className = 'form-control';
   txnRef.setAttribute('data-field', 'txnRef');
   txnRef.id = `inst_${year}_${rowIndexInYear}_txnRef`;
   tdTxnRef.appendChild(txnRef);
   tr.appendChild(tdTxnRef);

    // Receipt (file)
	const tdFile = document.createElement('td');

	// wrapper (hover area)
	const wrap = document.createElement('div');
	wrap.className = 'file-wrap';
	wrap.style.position = 'relative';

	// input
	const file = document.createElement('input');
	file.type = 'file';
	file.className = 'form-control';
	file.accept = '.pdf,.jpg,.jpeg,.png';
	file.setAttribute('data-field', 'receipt');
	file.id = `inst_${year}_${rowIndexInYear}_file`;

	// preview box
	const preview = document.createElement('div');
	preview.className = 'file-preview';

	wrap.appendChild(file);
	wrap.appendChild(preview);

	tdFile.appendChild(wrap);
	tr.appendChild(tdFile);

	let objUrl = null;

	file.addEventListener('change', () => {
	  const f = file.files && file.files[0];

	  // reset
	  wrap.classList.remove('has-file');
	  preview.innerHTML = '';
	  if (objUrl) URL.revokeObjectURL(objUrl);
	  objUrl = null;

	  if (!f) return;

	  wrap.classList.add('has-file');

	  objUrl = URL.createObjectURL(f);

	  const isImage = (f.type || '').startsWith('image/');
	  const isPdf = (f.type || '') === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf');

	  preview.innerHTML = `
	    <div class="fp-name">${f.name}</div>
	    ${isImage ? `<img class="fp-img" src="${objUrl}" alt="preview" />` : ''}
	    <div class="fp-meta">${(f.size/1024).toFixed(1)} KB • ${f.type || 'file'}</div>
	    ${isPdf ? `<a class="btn btn-sm btn-dark mt-2 w-100" href="${objUrl}" target="_blank" rel="noopener">View PDF</a>` : ''}
	  `;
	});

	

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
	
	// Invoice
    const tdInvoice = document.createElement('td');
    const invoiceRef = document.createElement('span');
   
    tdInvoice.appendChild(invoiceRef);
    tr.appendChild(tdInvoice);

    // 🔹 NEW: Actions (Delete button)
    const tdActions = document.createElement('td');
    if (canDeleteInstallment) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'btn btn-sm btn-outline-danger';
      btn.textContent = 'Delete';
      btn.addEventListener('click', () => handleDeleteInstallmentRow(tr));
      tdActions.appendChild(btn);
    }
    tr.appendChild(tdActions);

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

  /* ---------- equal split helpers ---------- */

  // 🔹 NEW: split for a specific year (used also after delete)
  function equalSplitUnpaidForYear(year) {
    const rowsUnpaid = unpaidRows(year);
    const n = rowsUnpaid.length;
    if (!n) { recalcInstallmentsSum(); return; }

    const targetBudget = getYearBudget(year);
    const paidSumY = paidAmountSum(year);
    let unpaidTotal = Math.max(targetBudget - paidSumY, 0);

    const base = Math.floor(unpaidTotal / n);
    let remainder = unpaidTotal - base * n;

    rowsUnpaid.forEach((tr, idx) => {
      const inp = tr.querySelector('input[data-field="amount"]');
      if (!inp) return;
      let val = base;
      if (idx === n - 1) { // leftover goes to LAST installment
        val += remainder;
        remainder = 0;
      }
      inp.value = val;
      inp.dataset.prev = String(val);
    });

    recalcInstallmentsSum();
  }

  // existing API: uses active year
  function equalSplitUnpaidAcrossCurrent() {
    const year = getActiveYear();
    equalSplitUnpaidForYear(year);
  }

  /* ---------- change number of rows ONLY in active year ---------- */
  function adjustRowsToCount(desiredCount) {
    const year = getActiveYear();
    const current = getRows(year).length;
    const paidCnt = getRows(year).filter(isPaidRow).length;
    const target  = Math.max(toNumber(desiredCount), paidCnt);

    if (target > current) {
      addUnpaidRows(target - current, year);
      equalSplitUnpaidForYear(year);
    } else if (target < current) {
      const leftover = removeUnpaidRowsFromBottom(current - target, year);
      if (leftover > 0) {
        countEl.value = String(getRows(year).length); // cannot go below paid
      }
      equalSplitUnpaidForYear(year);
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
  function addInstallment(opts = {}) {
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
  }

  function addInstallments(count, opts = {}) {
    const year = getActiveYear();
    const n = Math.max(0, Number(count) || 0);
    if (!n) return;
    for (let k = 0; k < n; k++) addInstallment({ ...opts, resplit: false });
    equalSplitUnpaidForYear(year);
    showYear(year);
  }

  // expose to window (for any external usage)
  window.addInstallment = addInstallment;
  window.addInstallments = addInstallments;

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

  // 🔥 Spread a delta in Actual Fees equally across *all* yearly budgets
  // and return a Map<year, deltaForThatYear>
  function adjustAllYearBudgetsByDelta(delta) {
    const years = getCourseYears();
    if (!Number.isFinite(delta) || delta === 0 || years <= 0) {
      return new Map();
    }

    // Ensure some budgets exist
    if (!yearBudgets || yearBudgets.size === 0) {
      computeDefaultBudgets();
    }

    const yearDeltas = new Map();
    const roundedDelta = Math.round(delta);

    if (roundedDelta > 0) {
      // ➕ Increase total actual fees: distribute increase equally across all years
      const abs = Math.abs(roundedDelta);
      const base = Math.floor(abs / years);
      let rem = abs - base * years;

      for (let y = 1; y <= years; y++) {
        let add = base;
        // leftover into LAST year
        if (y === years) {
          add += rem;
        }
        const oldBudget = getYearBudget(y);
        const newBudget = oldBudget + add;
        setYearBudget(y, newBudget);
        yearDeltas.set(y, add);
      }
    } else {
      // ➖ Decrease total actual fees (higher discount): subtract across all years
      let need = Math.abs(roundedDelta);

      // total capacity
      let totalCap = 0;
      for (let y = 1; y <= years; y++) {
        const currentBudget = getYearBudget(y);
        const paidY = paidAmountSum(y);
        const cap = Math.max(0, currentBudget - paidY);
        totalCap += cap;
      }

      if (totalCap === 0) {
        return new Map();
      }

      if (need > totalCap) {
        need = totalCap;
      }

      const base = Math.floor(need / years);
      let rem = need - base * years;

      for (let y = 1; y <= years; y++) {
        const currentBudget = getYearBudget(y);
        const paidY = paidAmountSum(y);
        const cap = Math.max(0, currentBudget - paidY);

        let dec = base;
        if (y === years) {
          dec += rem; // leftover reduction into last year
        }

        dec = Math.min(dec, cap);
        if (dec <= 0) {
          yearDeltas.set(y, 0);
          continue;
        }

        const newBudget = currentBudget - dec;
        setYearBudget(y, newBudget);
        yearDeltas.set(y, -dec);
        need -= dec;
      }
    }

    hasCustomBudgets = true;
    budgetsSource    = 'custom';

    return yearDeltas;
  }

  function cascadeDeltaFromYear(startYear, delta) {
    const years   = getCourseYears();
    const changed = new Set();

    if (!Number.isFinite(delta) || delta === 0 || years <= 1) {
      return [];
    }

    if (delta > 0) {
      // We increased startYear's budget → subtract this delta only from FUTURE years
      let need = delta;

      for (let y = startYear + 1; y <= years && need > 0; y++) {
        const cap  = maxSubtractableForYear(y);        // how much we can take without breaking paid
        if (cap <= 0) continue;

        const take = Math.min(cap, need);
        if (take > 0) {
          setYearBudget(y, getYearBudget(y) - take);
          changed.add(y);
          need -= take;
        }
      }

      // If `need` > 0 here, future years couldn't absorb full delta.
      // We leave it as is – total of budgets may be slightly > Actual, but past years are untouched.
    } else {
      // We decreased startYear's budget → we must ADD |delta| only to FUTURE years
      const addTotal = -delta;

      const futureYears = [];
      for (let y = startYear + 1; y <= years; y++) {
        futureYears.push(y);
      }

      if (futureYears.length === 0) {
        // No future years to adjust (startYear is last year) → nothing to cascade
        return [];
      }

      const base = Math.floor(addTotal / futureYears.length);
      let rem    = addTotal - base * futureYears.length;

      futureYears.forEach((y, idx) => {
        let add = base;
        if (idx === futureYears.length - 1) {
          add += rem; // leftover goes to last future year
        }
        if (add !== 0) {
          setYearBudget(y, getYearBudget(y) + add);
          changed.add(y);
        }
      });
    }

    hasCustomBudgets = true;
    budgetsSource    = 'custom';

    // Return list of future years whose budgets changed
    return Array.from(changed);
  }


  // 🔥 Spread a delta for a *single year* across that year's installments
  function adjustAllInstallmentsByDelta(delta, yearArg) {
    const year = yearArg || getActiveYear();
    const rowsUnpaid = unpaidRows(year); // only this year's UNPAID rows
    const n = rowsUnpaid.length;

    if (!n || !Number.isFinite(delta) || delta === 0) {
      recalcInstallmentsSum();
      return;
    }

    let d = Math.round(delta);

    // helper
    const getAmt = (tr) => tr.querySelector('input[data-field="amount"]');

    if (d > 0) {
      // ➕ Increase installments
      const base = Math.floor(d / n);
      let rem = d - base * n;
      let idx = 0;

      rowsUnpaid.forEach(tr => {
        const inp = getAmt(tr);
        if (!inp) return;
        let cur = toNumber(inp.value);

        let add = base;
        if (idx === n - 1) { // leftover to LAST
          add += rem;
        }

        const nextVal = cur + add;
        inp.value = String(nextVal);
        inp.dataset.prev = String(nextVal);
        idx++;
      });
    } else {
      // ➖ Decrease installments
      d = Math.abs(d);

      // Max we can subtract (don’t go below 0 on any installment)
      let totalCap = 0;
      rowsUnpaid.forEach(tr => {
        const inp = getAmt(tr);
        if (!inp) return;
        totalCap += Math.max(0, toNumber(inp.value));
      });

      let need = Math.min(d, totalCap);
      if (need <= 0) {
        recalcInstallmentsSum();
        return;
      }

      const base = Math.floor(need / n);
      let rem = need - base * n;
      let remaining = need;
      let idx = 0;

      rowsUnpaid.forEach(tr => {
        if (remaining <= 0) return;
        const inp = getAmt(tr);
        if (!inp) return;

        let cur = toNumber(inp.value);

        let take = base;
        if (idx === n - 1) { // leftover reduction into LAST
          take += rem;
        }

        take = Math.min(take, cur);   // don’t go negative
        const nextVal = cur - take;
        remaining -= take;

        inp.value = String(nextVal);
        inp.dataset.prev = String(nextVal);
        idx++;
      });
    }

    recalcInstallmentsSum();
  }

  function handleActiveYearBudgetEdit() {
    const y = getActiveYear();
    const oldBudget = getYearBudget(y);
    let newBudget   = toNumber(activeYearBudgetEl.value);

    // Never allow budget < already paid amount for that year
    const minAllowed = paidAmountSum(y);
    if (newBudget < minAllowed) {
      newBudget = minAllowed;
      activeYearBudgetEl.value = String(newBudget);
    }

    const delta = newBudget - oldBudget;
    if (!Number.isFinite(delta) || delta === 0) {
      return;
    }

    hasCustomBudgets = true;
    budgetsSource    = 'custom';

    // 1) Update active year's budget
    setYearBudget(y, newBudget);

    // 2) Cascade delta into other years and capture which years changed
    const impactedYears = cascadeDeltaFromYear(y, delta); // e.g. [2, 3]

    // 3) Re-split installments for the active year
    equalSplitUnpaidForYear(y);

    // 4) Re-split installments for all impacted years
    impactedYears.forEach(yr => {
      equalSplitUnpaidForYear(yr);
    });

    // 5) Refresh UI
    refreshActiveYearBudgetInput();
    recalcInstallmentsSum();
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
		  const dueDate = inst.dueDate || 0;
          const y   = inst.yearNumber && inst.yearNumber > 0 ? inst.yearNumber : 1;
          activeYearSel.value = String(y);
          const year = getActiveYear();
          addInstallment({ amount: amt, year: year ,date:dueDate});
        });

      // back to year 1
      activeYearSel.value = "1";
      showYear(1);

      recalcInstallmentsSum();
    } catch (e) {
      console.error("Error loading default fee structure", e);
    }
  }

  /* ---------- seed budgets from admission installments (backend) ---------- */
  /* ---------- seed budgets from admission installments (backend) ---------- */
  function seedBudgetsFromAdmission(list) {
    yearBudgets.clear();
    list.forEach(inst => {
      // Support both shapes: {yearNumber, amount} and {studyYear, amountDue}
      const y = (inst.yearNumber && inst.yearNumber > 0)
        ? inst.yearNumber
        : (inst.studyYear && inst.studyYear > 0)
          ? inst.studyYear
          : 1;

      const amt = (typeof inst.amount === 'number')
        ? inst.amount
        : (typeof inst.amountDue === 'number')
          ? inst.amountDue
          : 0;

      const prev = yearBudgets.get(y) || 0;
      yearBudgets.set(y, prev + amt);
    });

    initialYearBudgets = new Map(yearBudgets);
    budgetsSource = 'admission';
    hasCustomBudgets = false;
  }


  function clearAllInstallments() {
    getAllRows().forEach(tr => tr.remove());
    countEl.value = "0";

    if (sumEl) sumEl.value = 0;
    const yearSumEl = document.getElementById('yearInstallmentsSum');
    if (yearSumEl) yearSumEl.value = 0;

    recalcInstallmentsSum();
  }

  /* ---------- YEAR UI helpers ---------- */
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

  /* ---------- discount handler (GLOBAL, all years) ---------- */
  function handleDiscountChange() {
    // 1️⃣ Old Actual before discount change
    const oldActual = toNumber(actualFeesEl.value);

    const years = getCourseYears();

    // 2️⃣ Recalculate Actual (NO resplit of budgets here)
    recalcActual({ reSplit: false });

    // 3️⃣ Delta between new Actual and old Actual (across entire course)
    const newActual = toNumber(actualFeesEl.value);
    let deltaRaw = newActual - oldActual;   // may be + or -
    let delta    = Math.round(deltaRaw);    // integer rupees

    if (!Number.isFinite(delta) || delta === 0) {
      refreshActiveYearBudgetInput();
      recalcInstallmentsSum();
      return;
    }

    // 4️⃣ Adjust yearly budgets by this delta, *equally across all years*
    //    and get per-year deltas actually applied (Map<year, deltaY>)
    const yearDeltas = adjustAllYearBudgetsByDelta(delta);

    // 5️⃣ For each year, apply that year's delta equally to its installments.
    //    If it does not divide exactly, leftover rupees go to the LAST installment.
    for (let y = 1; y <= years; y++) {
      const dYear = yearDeltas.get(y) || 0;
      if (!dYear) continue;
      adjustAllInstallmentsByDelta(dYear, y);
    }

    // 6️⃣ Refresh UI
    refreshActiveYearBudgetInput();
    recalcInstallmentsSum();
  }

  /* ---------- wire events ---------- */
  totalFeesEl.addEventListener('input', () =>
    recalcActual({ reSplit: budgetsSource === 'auto' })
  );
  discountEl.addEventListener('input', handleDiscountChange);

  countEl.addEventListener('change', e => adjustRowsToCount(e.target.value));
  evenSplitBtn?.addEventListener('click', equalSplitUnpaidAcrossCurrent);

  courseYearsEl.addEventListener('change', () => ensureYearOptions());
  activeYearSel.addEventListener('change', () => showYear(getActiveYear()));

  ['change', 'blur', 'input'].forEach(evt => {
    activeYearBudgetEl.addEventListener(evt, handleActiveYearBudgetEdit);
  });

  /* ---------- existing admission – don't override with template ---------- */
  $("body").on("change", "#course", function () {
    let selectedCourseId = $(this).val();
    selectedCourseId = Number.parseInt(selectedCourseId);
	debugger
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
		//activeYearSel
		const event = new Event('change', { bubbles: true });
		activeYearSel.dispatchEvent(event);
		//showYear()
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

  /* ---------- initial pass ---------- */
  /* ---------- initial pass ---------- */
  getAllRows().forEach(tr => {
    // Make sure each row has a year tag
    if (!tr.dataset.year) tr.dataset.year = '1';

    lockRowIfPaid(tr);

    const amt = tr.querySelector('input[data-field="amount"]');
    if (amt && amt.dataset.prev == null) {
      amt.dataset.prev = String(toNumber(amt.value));
    }
  });

  // Detect if this is an existing admission with installments from DB
  const isExistingAdmission =
    hasExistingInstallments &&
    Array.isArray(existingInstallments) &&
    existingInstallments.length > 0;

  if (isExistingAdmission) {
    // 1️⃣ Seed year budgets from DB installments
    seedBudgetsFromAdmission(existingInstallments);

    // 2️⃣ Build year dropdown WITHOUT touching budgets
    ensureYearOptions({ skipBudgetRecalc: true });
  } else {
    // New admission – auto budgets from total + years
    recalcActual({ reSplit: true });
    ensureYearOptions();
  }

  // 3️⃣ Ensure an active year is selected (default = 1)
  if (!activeYearSel.value) {
    activeYearSel.value = '1';
  }

  // 4️⃣ Normalize any rows that still don't have year
  getAllRows().forEach(tr => {
    if (!tr.dataset.year) {
      tr.dataset.year = activeYearSel.value || '1';
    }
  });

  // 5️⃣ Show active year & FORCE initial totals calculation
  showYear(getActiveYear());   // this internally calls recalcInstallmentsSum()
  recalcInstallmentsSum();     // extra safety to ensure Overall Total is set
  refreshActiveYearBudgetInput();

})();
