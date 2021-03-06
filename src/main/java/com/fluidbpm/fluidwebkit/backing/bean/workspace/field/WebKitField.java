package com.fluidbpm.fluidwebkit.backing.bean.workspace.field;

import com.fluidbpm.program.api.util.GeoUtil;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.faces.model.SelectItem;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class WebKitField extends Field {

	@Getter
	@Setter
	private boolean mandatoryAndEmpty;

	public WebKitField(
		Long fieldIdParam,
		String fieldNameParam,
		Object fieldValueParam,
		Type fieldTypeParam
	) {
		super(fieldIdParam);
		this.setFieldName(fieldNameParam);
		this.setTypeAsEnum(fieldTypeParam);
		this.setFieldValue(fieldValueParam);
	}

	@XmlTransient
	public SelectItem getSelectedMultiChoiceSelectItem() {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return null;
		
		return new SelectItem(multi.getSelectedMultiChoice(), multi.getSelectedMultiChoice());
	}

	@XmlTransient
	public void setSelectedMultiChoiceSelectItem(SelectItem selectItem) {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return;
		
		multi.setSelectedMultiChoice(selectItem.getValue() == null ? null : selectItem.getValue().toString());
	}

	@XmlTransient
	public List<SelectItem> getActionFetchAllAvailMultiChoice() {
		List<SelectItem> returnList = new ArrayList();
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;
		
		List<String> availSelectItems = multi.getAvailableMultiChoices();
		if (availSelectItems == null) return returnList;
		return new ArrayList<>(availSelectItems.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList()));
	}
	
	@XmlTransient
	public List<SelectItem> autoCompleteWithQuery(String itemQueryParam) {
		List<SelectItem> returnList = new ArrayList();

		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;

		List<String> availSelectItems = multi.getAvailableMultiChoices();
		List<SelectItem> availSelectItemsAsSE = new ArrayList<>(availSelectItems.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList()));

		if (UtilGlobal.isBlank(itemQueryParam)) {
			returnList.addAll(availSelectItemsAsSE);
			return returnList;
		}

		String itemQueryParamLower = itemQueryParam.toLowerCase();
		for (SelectItem toCheck : availSelectItemsAsSE) {
			String label = toCheck.getLabel();

			if (UtilGlobal.isBlank(label)) continue;

			if (toCheck.getValue() == null || !(toCheck.getValue() instanceof String)) continue;

			String stringValue = (String)toCheck.getValue();
			if (UtilGlobal.isBlank(stringValue)) continue;

			String labelLower = label.toLowerCase();
			if (labelLower.indexOf(itemQueryParamLower) > -1) returnList.add(toCheck);
		}

		return returnList;
	}

	@XmlTransient
	public List<String> autoCompleteWithQueryString(String itemQueryParam) {
		List<String> returnList = new ArrayList();

		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;

		List<String> availSelectItems = multi.getAvailableMultiChoices();

		if (UtilGlobal.isBlank(itemQueryParam)) {
			returnList.addAll(availSelectItems);
			return returnList;
		}

		String itemQueryParamLower = itemQueryParam.toLowerCase();
		for (String toCheck : availSelectItems) {
			if (UtilGlobal.isBlank(toCheck)) continue;

			String labelLower = toCheck.toLowerCase();
			if (labelLower.indexOf(itemQueryParamLower) > -1) returnList.add(toCheck);
		}

		return returnList;
	}

	@XmlTransient
	public int getInputValueColumnCount() {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null || multi.getAvailableMultiChoices() == null) return 1;

		int count = multi.getAvailableMultiChoices().size();

		if (count < 1) return 1;

		int returnVal = (int)Math.sqrt(count);

		if (returnVal > 3) returnVal = 3;

		return returnVal;
	}

	@XmlTransient
	public Field asField() {
		return this;
	}

	@XmlTransient
	public GeoUtil getFieldValueAsGeo() {
		return new GeoUtil(this.getFieldValueAsString());
	}

	@XmlTransient
	public void setFieldValueAsGeo(GeoUtil geo) {
		if (geo == null) {
			this.setFieldValue(null);
			return;
		}
		this.setFieldValue(geo.toString());
	}

	@XmlTransient
	public boolean isFieldValueEmpty() {
		switch (this.getTypeAsEnum()) {
			case Text:
			case ParagraphText:
			case TextEncrypted:
				String textVal = this.getFieldValueAsString();
				if (textVal == null || textVal.trim().isEmpty()) return true;
				else return false;
			case Decimal:
				Double fieldVal = this.getFieldValueAsDouble();
				if (fieldVal == null || fieldVal.doubleValue() == 0.0D) return true;
				else return false;
			case DateTime:
				Date dateVal = this.getFieldValueAsDate();
				if (dateVal == null) return true;
				else return false;
			case MultipleChoice:
				MultiChoice mc = this.getFieldValueAsMultiChoice();
				if ((mc.getSelectedMultiChoice() == null || mc.getSelectedMultiChoice().isEmpty()) &&
				mc.getSelectedMultiChoices() == null || mc.getSelectedMultiChoices().isEmpty()) return true;
				else return false;
			default:
				return false;
		}

	}
}
