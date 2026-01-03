
$(document).ready(function () {
	
	function generateInvoiceForRow($row) {
			    const status = "Paid"
				debugger
			    const installmentId = $row.find("input[name$='.installmentId']").val();

			    if (!installmentId) {
			      console.warn("No installmentId found on row, probably a new unsaved installment.");
			      return;
			    }

			    if (status !== "Paid") {
			      return;
			    }

			   

			    $.ajax({
			      url: "/api/installments/" + installmentId + "/status?status=" + status,
			      type: "POST",
			      contentType: "application/json",
			      data: JSON.stringify({ status: "Paid" }),
			      success: function (res) {
			        const resolvedStatus = res && res.status ? res.status : "Paid";
			        const $statusSelect = $row.find("select[data-field='status']");
			        const $statusHidden = $row.find("input[data-field='status']");
			        if ($statusSelect.length) {
			          $statusSelect.val(resolvedStatus).prop("disabled", true);
			        }
			        if ($statusHidden.length) {
			          $statusHidden.val(resolvedStatus);
			          const $statusCell = $statusHidden.closest("td");
			          if ($statusCell.length) {
			            $statusCell.text(resolvedStatus).removeClass("text-warning text-info text-success fw-semibold");
			            if (resolvedStatus === "Paid") {
			              $statusCell.addClass("text-success fw-semibold");
			            } else if (resolvedStatus === "Partial Received") {
			              $statusCell.addClass("text-info fw-semibold");
			            }
			          }
			        }
			        $row.find(".installment_apporval_btn").remove();
			        $row.find(".installment_delete_btn").prop("disabled", true);
			      },
			      error: function (xhr) {
			        console.error("Error marking installment paid:", xhr);
			        alert("Failed to generate invoice. Please try again.");
			      }
			    });
			  }
	
	$("body").on("click",".installment_apporval_btn",function(e){
		let installmentId= this.id
		
		installmentId = installmentId.split("_")[2]
		let currentTr = $(this).closest('tr')
		generateInvoiceForRow(currentTr)
	})
})
