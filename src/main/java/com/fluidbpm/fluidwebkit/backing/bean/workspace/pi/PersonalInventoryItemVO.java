package com.fluidbpm.fluidwebkit.backing.bean.workspace.pi;

import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PersonalInventoryItemVO extends ABaseWebVO {
	private String title;
	private String step;
	private String state;
	private String type;
	private String currentUser;
	private Long fluidItemId;
	private Long formContainerId;
	private Date dateLastUpdated;
	private Date dateCreated;

	public static final String COMPLETED_NOT_IN_FLOW_SENT = "[Completed / Not In Flow]";
	public static final String PLACEHOLDER_TITLE = "[[PLACEHOLDER]]";

	public PersonalInventoryItemVO(FluidItem fluidItm) {
		super(fluidItm);
		Form form = this.getForm();
		this.fluidItemId = fluidItm.getId();
		this.title = form.getTitle();
		this.formContainerId = form.getId();
		this.dateLastUpdated = form.getDateLastUpdated();
		this.dateCreated = form.getDateCreated();

		this.step = COMPLETED_NOT_IN_FLOW_SENT;
		if (fluidItm.getStep() != null && !fluidItm.getStep().trim().isEmpty()) this.step = fluidItm.getStep();

		this.type = form.getFormType();
		this.state = form.getState();

		this.currentUser = (form.getCurrentUser() == null ||
				UtilGlobal.isBlank(form.getCurrentUser().getUsername())) ? "[None]" :
				form.getCurrentUser().getUsername();
	}

	public PersonalInventoryItemVO cloneVO(
		FluidItem fluidItem,
		List<Field> fieldsViewable,
		List<Field> fieldsEditable,
		List<User> allUsers
	) {
		PersonalInventoryItemVO returnVal = new PersonalInventoryItemVO(fluidItem);
		returnVal.setFieldsViewable(fieldsViewable);
		returnVal.setFieldsEditable(fieldsEditable);
		returnVal.setAllUsers(allUsers);
		return returnVal;
	}
}
