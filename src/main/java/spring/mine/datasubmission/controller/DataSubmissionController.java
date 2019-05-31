package spring.mine.datasubmission.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.controller.BaseController;
import spring.mine.datasubmission.form.DataSubmissionForm;
import spring.mine.datasubmission.validator.DataSubmissionFormValidator;
import spring.mine.internationalization.MessageUtil;
import spring.service.datasubmission.DataIndicatorService;
import spring.service.datasubmission.TypeOfDataIndicatorService;
import spring.service.siteinformation.SiteInformationService;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.validator.GenericValidator;
import us.mn.state.health.lims.datasubmission.DataIndicatorFactory;
import us.mn.state.health.lims.datasubmission.DataSubmitter;
import us.mn.state.health.lims.datasubmission.valueholder.DataIndicator;
import us.mn.state.health.lims.datasubmission.valueholder.TypeOfDataIndicator;
import us.mn.state.health.lims.siteinformation.valueholder.SiteInformation;

@Controller
public class DataSubmissionController extends BaseController {

	@Autowired
	DataSubmissionFormValidator formValidator;
	@Autowired
	SiteInformationService siteInformationService;
	@Autowired
	TypeOfDataIndicatorService typeOfDataIndicatorService;
	@Autowired
	DataIndicatorService dataIndicatorService;

	@RequestMapping(value = "/DataSubmission", method = RequestMethod.GET)
	public ModelAndView showDataSubmission(HttpServletRequest request) {
		DataSubmissionForm form = new DataSubmissionForm();

		int month = GenericValidator.isBlankOrNull(request.getParameter("month")) ? DateUtil.getCurrentMonth() + 1
				: Integer.parseInt(request.getParameter("month"));
		int year = GenericValidator.isBlankOrNull(request.getParameter("year")) ? DateUtil.getCurrentYear()
				: Integer.parseInt(request.getParameter("year"));

		List<DataIndicator> indicators = new ArrayList<>();
		List<TypeOfDataIndicator> typeOfIndicatorList = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
		for (TypeOfDataIndicator typeOfIndicator : typeOfIndicatorList) {
			DataIndicator indicator = dataIndicatorService.getIndicatorByTypeYearMonth(typeOfIndicator, year, month);
			if (indicator == null) {
				indicator = DataIndicatorFactory.createBlankDataIndicatorForType(typeOfIndicator);
			}
			indicator.setYear(year);
			indicator.setMonth(month);
			indicators.add(indicator);
		}

		form.setDataSubUrl(siteInformationService.getSiteInformationByName("Data Sub URL"));
		form.setIndicators(indicators);
		form.setMonth(month);
		form.setYear(year);
		form.setSiteId(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));

		addFlashMsgsToRequest(request);
		return findForward(FWD_SUCCESS, form);
	}

	@RequestMapping(value = "/DataSubmission", method = RequestMethod.POST)
	public ModelAndView showDataSubmissionSave(HttpServletRequest request,
			@ModelAttribute("form") @Validated(DataSubmissionForm.DataSubmission.class) DataSubmissionForm form,
			BindingResult result, RedirectAttributes redirectAttributes) throws IOException, ParseException {
		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}

		int month = form.getMonth();
		int year = form.getYear();
		@SuppressWarnings("unchecked")
		List<DataIndicator> indicators = (List<DataIndicator>) form.get("indicators");
		boolean submit = "true".equalsIgnoreCase(request.getParameter("submit"));
		SiteInformation dataSubUrl = (SiteInformation) form.get("dataSubUrl");
		dataSubUrl = (SiteInformation) siteInformationService.getSiteInformationByDomainName("Data Sub URL");
		dataSubUrl.setValue(form.getDataSubUrl().getValue());
		dataSubUrl.setSysUserId(getSysUserId(request));
		siteInformationService.updateData(dataSubUrl);
		for (DataIndicator indicator : indicators) {
			if (submit && indicator.isSendIndicator()) {
				boolean success = DataSubmitter.sendDataIndicator(indicator);
				indicator.setStatus(DataIndicator.SENT);
				if (success) {
					indicator.setStatus(DataIndicator.RECEIVED);
				} else {
					indicator.setStatus(DataIndicator.FAILED);
					result.reject("errors.IndicatorCommunicationException",
							new String[] { MessageUtil.getMessage(indicator.getTypeOfIndicator().getNameKey()) },
							"errors.IndicatorCommunicationException");
				}
			}

			DataIndicator databaseIndicator = dataIndicatorService.getIndicatorByTypeYearMonth(indicator.getTypeOfIndicator(),
					year, month);
			if (databaseIndicator == null) {
				dataIndicatorService.insertData(indicator);
			} else {
				indicator.setId(databaseIndicator.getId());
				dataIndicatorService.updateData(indicator);
			}
		}

		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}
		redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
		return findForward(FWD_SUCCESS_INSERT, form);
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "dataSubmissionDefinition";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "redirect:/DataSubmission.do";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "dataSubmissionDefinition";
		} else {
			return "PageNotFound";
		}
	}

	@Override
	protected String getPageTitleKey() {
		return "datasubmission.browse.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "datasubmission.browse.title";
	}
}
