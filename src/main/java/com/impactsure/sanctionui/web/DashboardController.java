package com.impactsure.sanctionui.web;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.dto.CollegeCourseSeatDto;
import com.impactsure.sanctionui.dto.CollegeDto;
import com.impactsure.sanctionui.repository.Admission2Repository;
import com.impactsure.sanctionui.repository.CourseRepository;
import com.impactsure.sanctionui.repository.StudentRepository;
import com.impactsure.sanctionui.service.impl.CollegeApiClientService;

@Controller
public class DashboardController {

	@Autowired
	private Admission2Repository admission2Repository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private CollegeApiClientService collegeApiClientService;

	@GetMapping("/dashboard")
	public String dashboard(
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser,
			Model model
	) {
		String accessToken = client.getAccessToken().getTokenValue();

		long studentCount = studentRepository.count();
		long admissionCount = admission2Repository.count();
		long courseCount = courseRepository.count();
		List<CollegeDto> colleges = collegeApiClientService.listAll(accessToken);
		int collegeCount = colleges.size();

		List<Admission2> allAdmissions = admission2Repository.findAll();
		BigDecimal totalFees = BigDecimal.ZERO;
		BigDecimal paidFees = BigDecimal.ZERO;

		for (Admission2 admission : allAdmissions) {
			if (admission.getTotalFees() != null) {
				totalFees = totalFees.add(BigDecimal.valueOf(admission.getTotalFees()));
			}
			if (admission.getInstallments() != null) {
				BigDecimal admissionPaid = admission.getInstallments().stream()
						.filter(inst -> inst.getStatus() != null
								&& inst.getStatus().equalsIgnoreCase("Paid"))
						.map(inst -> inst.getAmountDue() == null ? BigDecimal.ZERO : inst.getAmountDue())
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				paidFees = paidFees.add(admissionPaid);
			}
		}

		BigDecimal pendingFees = totalFees.subtract(paidFees);
		if (pendingFees.compareTo(BigDecimal.ZERO) < 0) {
			pendingFees = BigDecimal.ZERO;
		}

		List<Admission2> recentAdmissions = admission2Repository.findAll(
				PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt", "admissionId"))
		).getContent();

		Map<String, Long> topCourses = allAdmissions.stream()
				.filter(a -> a.getCourse() != null && a.getCourse().getName() != null)
				.collect(Collectors.groupingBy(a -> a.getCourse().getName(), Collectors.counting()));
		Map<String, Long> topCourseSorted = topCourses.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
				.limit(5)
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(a, b) -> a,
						LinkedHashMap::new
				));

		List<CollegeSeatSummary> allCollegeSummaries = colleges.stream()
				.map(college -> summarizeCollegeSeats(college, accessToken))
				.collect(Collectors.toList());

		SeatUtilizationSummary seatUtilization = summarizeOverallSeats(allCollegeSummaries);
		List<CollegeSeatSummary> collegeSeatSummaries = allCollegeSummaries.stream()
				.sorted(Comparator.comparingInt(CollegeSeatSummary::getUtilizedSeats).reversed())
				.limit(6)
				.collect(Collectors.toList());
		List<CollegeSeatSummary> collegeCourseSummaries = allCollegeSummaries.stream()
				.filter(summary -> summary.getCourseSummaries() != null && !summary.getCourseSummaries().isEmpty())
				.sorted(Comparator.comparingInt(CollegeSeatSummary::getUtilizedSeats).reversed())
				.limit(4)
				.collect(Collectors.toList());

		model.addAttribute("studentCount", studentCount);
		model.addAttribute("admissionCount", admissionCount);
		model.addAttribute("courseCount", courseCount);
		model.addAttribute("collegeCount", collegeCount);
		model.addAttribute("totalFees", totalFees);
		model.addAttribute("paidFees", paidFees);
		model.addAttribute("pendingFees", pendingFees);
		model.addAttribute("recentAdmissions", recentAdmissions);
		model.addAttribute("topCourses", topCourseSorted);
		model.addAttribute("collegeSeatSummaries", collegeSeatSummaries);
		model.addAttribute("collegeCourseSummaries", collegeCourseSummaries);
		model.addAttribute("seatUtilization", seatUtilization);

		return "dashboard";
	}

	private CollegeSeatSummary summarizeCollegeSeats(CollegeDto college, String accessToken) {
		List<CollegeCourseSeatDto> seats;
		try {
			seats = collegeApiClientService.getCollegeCourseSeats(college.getCollegeId(), accessToken);
		} catch (Exception ex) {
			seats = List.of();
		}
		int total = 0;
		int utilized = 0;
		int onHold = 0;
		int remaining = 0;
		for (CollegeCourseSeatDto seat : seats) {
			total += safeInt(seat.getTotalSeats());
			utilized += safeInt(seat.getUtilizedSeats());
			onHold += safeInt(seat.getOnHoldSeats());
			remaining += safeInt(seat.getRemainingSeats());
		}
		List<CourseSeatSummary> courseSummaries = seats.stream()
				.filter(seat -> seat.getCourseName() != null)
				.map(seat -> {
					int courseTotal = safeInt(seat.getTotalSeats());
					int courseUtilized = safeInt(seat.getUtilizedSeats());
					int courseOnHold = safeInt(seat.getOnHoldSeats());
					int courseRemaining = safeInt(seat.getRemainingSeats());
					int percent = courseTotal == 0 ? 0 : (int) Math.round((courseUtilized * 100.0) / courseTotal);
					return new CourseSeatSummary(seat.getCourseName(), courseTotal, courseUtilized, courseOnHold,
							courseRemaining, percent);
				})
				.sorted(Comparator.comparingInt(CourseSeatSummary::getUtilizedSeats).reversed())
				.limit(3)
				.collect(Collectors.toList());
		int utilizationPercent = total == 0 ? 0 : (int) Math.round((utilized * 100.0) / total);
		return new CollegeSeatSummary(college.getName(), total, utilized, onHold, remaining, utilizationPercent,
				courseSummaries);
	}

	private SeatUtilizationSummary summarizeOverallSeats(List<CollegeSeatSummary> summaries) {
		int total = 0;
		int utilized = 0;
		int onHold = 0;
		int remaining = 0;
		for (CollegeSeatSummary summary : summaries) {
			total += summary.getTotalSeats();
			utilized += summary.getUtilizedSeats();
			onHold += summary.getOnHoldSeats();
			remaining += summary.getRemainingSeats();
		}
		int utilizationPercent = total == 0 ? 0 : (int) Math.round((utilized * 100.0) / total);
		return new SeatUtilizationSummary(total, utilized, onHold, remaining, utilizationPercent);
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value;
	}

	public static class CollegeSeatSummary {
		private final String collegeName;
		private final int totalSeats;
		private final int utilizedSeats;
		private final int onHoldSeats;
		private final int remainingSeats;
		private final int utilizationPercent;
		private final List<CourseSeatSummary> courseSummaries;

		public CollegeSeatSummary(String collegeName, int totalSeats, int utilizedSeats,
				int onHoldSeats, int remainingSeats, int utilizationPercent,
				List<CourseSeatSummary> courseSummaries) {
			this.collegeName = collegeName;
			this.totalSeats = totalSeats;
			this.utilizedSeats = utilizedSeats;
			this.onHoldSeats = onHoldSeats;
			this.remainingSeats = remainingSeats;
			this.utilizationPercent = utilizationPercent;
			this.courseSummaries = courseSummaries;
		}

		public String getCollegeName() { return collegeName; }
		public int getTotalSeats() { return totalSeats; }
		public int getUtilizedSeats() { return utilizedSeats; }
		public int getOnHoldSeats() { return onHoldSeats; }
		public int getRemainingSeats() { return remainingSeats; }
		public int getUtilizationPercent() { return utilizationPercent; }
		public List<CourseSeatSummary> getCourseSummaries() { return courseSummaries; }
	}

	public static class CourseSeatSummary {
		private final String courseName;
		private final int totalSeats;
		private final int utilizedSeats;
		private final int onHoldSeats;
		private final int remainingSeats;
		private final int utilizationPercent;

		public CourseSeatSummary(String courseName, int totalSeats, int utilizedSeats,
				int onHoldSeats, int remainingSeats, int utilizationPercent) {
			this.courseName = courseName;
			this.totalSeats = totalSeats;
			this.utilizedSeats = utilizedSeats;
			this.onHoldSeats = onHoldSeats;
			this.remainingSeats = remainingSeats;
			this.utilizationPercent = utilizationPercent;
		}

		public String getCourseName() { return courseName; }
		public int getTotalSeats() { return totalSeats; }
		public int getUtilizedSeats() { return utilizedSeats; }
		public int getOnHoldSeats() { return onHoldSeats; }
		public int getRemainingSeats() { return remainingSeats; }
		public int getUtilizationPercent() { return utilizationPercent; }
	}

	public static class SeatUtilizationSummary {
		private final int totalSeats;
		private final int utilizedSeats;
		private final int onHoldSeats;
		private final int remainingSeats;
		private final int utilizationPercent;

		public SeatUtilizationSummary(int totalSeats, int utilizedSeats, int onHoldSeats,
				int remainingSeats, int utilizationPercent) {
			this.totalSeats = totalSeats;
			this.utilizedSeats = utilizedSeats;
			this.onHoldSeats = onHoldSeats;
			this.remainingSeats = remainingSeats;
			this.utilizationPercent = utilizationPercent;
		}

		public int getTotalSeats() { return totalSeats; }
		public int getUtilizedSeats() { return utilizedSeats; }
		public int getOnHoldSeats() { return onHoldSeats; }
		public int getRemainingSeats() { return remainingSeats; }
		public int getUtilizationPercent() { return utilizationPercent; }
	}
}
