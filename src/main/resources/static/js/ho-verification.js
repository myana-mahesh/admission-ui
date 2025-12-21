
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
			        if (res && res.downloadUrl) {
			          let $invoiceCell = $row.find(".invoice-cell");
			          if ($invoiceCell.length === 0) {
			            // fallback: use receipt column (index adjust if needed)
			            $invoiceCell = $row.find("td").eq(4);
			          }
			          $invoiceCell.append(
			            `<div class="mt-1">
			               <a href="${res.downloadUrl}"
			                  target="_blank"
			                  class="text-decoration-none invoice-link">
			                 <i class="bi bi-file-earmark-pdf me-1"></i>
			                 Invoice
			               </a>
			             </div>`
			          );
			        } else {
			          console.warn("Installment marked as Paid, but invoice info missing.");
			        }
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
		alert(installmentId)
		generateInvoiceForRow(currentTr)
	})
})
