package com.lfdb.parapesquisa.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfdb.parapesquisa.DateParser;
import com.lfdb.parapesquisa.InvalidDateException;
import com.lfdb.parapesquisa.api.descriptors.FormInfo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Created by Igor on 7/29/13.
 */
public class UPPSForm {
    public enum InputType {
        Text,
        Number,
        Radio,
        Check,
        Age,
        Date,
        OrderedList,
        Private,
        Select,
        Label,
        Currency,

        Cpf,
        Email,
        Url
    }
    public enum InputLayout {
        Small,
        Medium,
        Big,
        SingleColumn,
        MultipleColumns
    }
    public abstract class Input {
        public long local_id;

        public String name;
        public String label;
        public String hint;
        public FormInfo.Section.Field data;
        public boolean isIdentifier;
        public boolean readonly;
        public InputLayout layout;
        public int order;

        public boolean required;
        public abstract InputType getType();
        public Vector<InputAction> actions = new Vector<InputAction>();
    }
    public class TextInput extends Input {
        public String value;

        public InputType getType() { return InputType.Text; }
    }
    public class AgeInput extends Input {
        public Integer value;

        public InputType getType() { return InputType.Age; }
    }
    public class RadioInput extends Input {
        public Vector<RadioOption> options = new Vector<RadioOption>();
        public InputType getType() { return InputType.Radio; }
    }
    public class DateInput extends Input {
        public Date value;

        public InputType getType() { return InputType.Date; }
    }
    public class NumberInput extends Input {
        public Integer value;

        public InputType getType() { return InputType.Number; }
    }
    public class OrderedListInput extends Input {
        public Vector<OrderedListOption> options = new Vector<OrderedListOption>();
        public InputType getType() { return InputType.OrderedList; }
    }
    public class CheckInput extends Input {
        public Vector<CheckOption> options = new Vector<CheckOption>();
        public InputType getType() { return InputType.Check; }
    }
    public class PrivateInput extends Input {
        public InputType getType() { return InputType.Private; }
    }
    public class CPFInput extends Input {
        public InputType getType() { return InputType.Cpf; }
    }
    public class EmailInput extends Input {
        public InputType getType() { return InputType.Email; }
    }
    public class URLInput extends Input {
        public InputType getType() { return InputType.Url; }
    }
    public class SelectInput extends Input {
        public Vector<SelectOption> options = new Vector<SelectOption>();
        public InputType getType() { return InputType.Select; }
    }
    public class LabelInput extends Input {
        public InputType getType() { return InputType.Label; }
    }
    public class CurrencyInput extends Input {
        public InputType getType() { return InputType.Currency; }
    }
    public class InputAction {
        public Object when[];
        public String enable[];
        public String disable[];
    }
    public class RadioOption {
        //public int id;
        public String label;
        public String value;
    }
    public class CheckOption {
        //public int id;
        public String label;
        public String value;
    }
    public class OrderedListOption {
        public String label;
        public String value;
    }
    public class SelectOption {
        public String label;
        public String value;
    }

    class FormInputComparator implements Comparator<Input> {

        @Override
        public int compare(UPPSForm.Input page, UPPSForm.Input page2) {
            return ((Integer)page.order).compareTo(page2.order);
        }
    }

    public class Page {
        public long local_id;

        public int id;
        public int order;
        public String title;
        public Vector<Input> inputs = new Vector<Input>();

        public int getInputIndex(String name) {
            Collections.sort(inputs, new FormInputComparator());

            int i = 0;
            for(Input input : inputs) {
                if(input.readonly)
                    continue;

                if(input.name.equals(name))
                    return i;

                i++;
            }

            return -1;
        }

        public Input addTextInput(String name, String label, String hint) {
            Input i = this.addTextInput(name, label);
            i.hint = hint;

            return i;
        }

        public Input addTextInput(String name, String label) {
            TextInput input = new TextInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addInput(String name, LinkedHashMap data) {
            /*Integer id = (Integer)data.get("id");
            String label = (String)data.get("label");
            String description = (String)data.get("description");
            Boolean identifier = (Boolean)data.get("identifier");
            Boolean read_only = (Boolean)data.get("read_only");
            String type = (String)data.get("type");
            ArrayList<LinkedHashMap> validations = (ArrayList<LinkedHashMap>)data.get("validations");*/

            ObjectMapper mapper = new ObjectMapper();
            FormInfo.Section.Field _data = mapper.convertValue(data, FormInfo.Section.Field.class);

            return addInput(name, _data);
        }

        public Input addInput(String name, FormInfo.Section.Field data) {
            Input i = addInput(data.type, name, data.label, data);
            if(i != null) {
                i.data = data;//.toString();
                i.hint = data.description;
                i.isIdentifier = data.identifier;
                i.readonly = data.read_only;
                if(data.actions != null) {
                    for(int j = 0; j < data.actions.length; j++) {
                        FormInfo.Section.Field.Action actiondata = data.actions[j];
                        String[] when = actiondata.when;
                        int[] disable = actiondata.disable;

                        if(when == null)
                            continue;

                        InputAction action = new InputAction();
                        action.when = new Object[when.length];
                        for(int k = 0; k < when.length; k++) {
                            action.when[k] = when[k];
                        }

                        if(disable != null) {
                            action.disable = new String[disable.length];
                            for(int k = 0; k < disable.length; k++) {
                                action.disable[k] = Integer.toString(disable[k]);
                            }
                        }

                        i.actions.add(action);
                    }
                }
                if(data.layout != null) {
                    if(data.layout.equals("single_column")) {
                        i.layout = InputLayout.SingleColumn;
                    } else if(data.layout.equals("multiple_columns")) {
                        i.layout = InputLayout.MultipleColumns;
                    }
                }
                i.order = data.order;
            }

            return i;
        }

        public Input addInput(String type, String name, String label, FormInfo.Section.Field data) {
            if(type.equals("TextField")) {
                return addTextInput(name, label);
            } else if(type.equals("NumberField")) {
                return addNumberInput(name, label);
            } else if(type.equals("RadioField")) {
                return addRadioInput(name, label, data);
            } else if(type.equals("DatetimeField")) {
                return addDateInput(name, label);
            } else if(type.equals("OrderedlistField")) {
                return addOrderedListInput(name, label, data);
            } else if(type.equals("CheckboxField")) {
                return addCheckInput(name, label, data);
            } else if(type.equals("PrivateField")) {
                return addPrivateInput(name, label);
            } else if(type.equals("CpfField")) {
                return addCpfInput(name, label);
            } else if(type.equals("EmailField")) {
                return addEmailInput(name, label);
            } else if(type.equals("UrlField")) {
                return addUrlInput(name, label);
            } else if(type.equals("SelectField")) {
                return addSelectInput(name, label, data);
            } else if(type.equals("LabelField")) {
                return addLabelInput(name, label);
            } else if(type.equals("DinheiroField")) {
                return addCurrencyInput(name, label);
            }

            return null;
        }

        public Input addCurrencyInput(String name, String label) {
            CurrencyInput input = new CurrencyInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addLabelInput(String name, String label) {
            LabelInput input = new LabelInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addSelectInput(String name, String label, FormInfo.Section.Field data) {
            SelectInput input = new SelectInput();
            input.name = name;
            input.label = label;

            if(data.options != null) {
                for(int i = 0; i < data.options.length; i++) {
                    FormInfo.Section.Field.Option optiondata = data.options[i];

                    SelectOption option = new SelectOption();
                    option.label = optiondata.label;
                    option.value = optiondata.value;
                    input.options.add(option);
                }
            }

            inputs.add(input);
            return input;
        }

        public Input addRadioInput(String name, String label, FormInfo.Section.Field data) {
            RadioInput input = new RadioInput();
            input.name = name;
            input.label = label;

            if(data.options != null) {
                for(int i = 0; i < data.options.length; i++) {
                    FormInfo.Section.Field.Option optiondata = data.options[i];

                    RadioOption option = new RadioOption();
                    option.label = optiondata.label;
                    option.value = optiondata.value;
                    input.options.add(option);
                }
            }

            inputs.add(input);
            return input;
        }

        public Input addCheckInput(String name, String label, FormInfo.Section.Field data) {
            CheckInput input = new CheckInput();
            input.name = name;
            input.label = label;

            if(data.options != null) {
                for(int i = 0; i < data.options.length; i++) {
                    FormInfo.Section.Field.Option optiondata = data.options[i];

                    CheckOption option = new CheckOption();
                    option.label = optiondata.label;
                    option.value = optiondata.value;
                    input.options.add(option);
                }
            }

            inputs.add(input);
            return input;
        }

        public Input addAgeInput(String name, String label, String hint) {
            Input i = this.addAgeInput(name, label);
            i.hint = hint;

            return i;
        }

        public Input addAgeInput(String name, String label) {
            AgeInput input = new AgeInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addCpfInput(String name, String label) {
            CPFInput input = new CPFInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addEmailInput(String name, String label) {
            EmailInput input = new EmailInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addUrlInput(String name, String label) {
            URLInput input = new URLInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addPrivateInput(String name, String label) {
            PrivateInput input = new PrivateInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addNumberInput(String name, String label) {
            NumberInput input = new NumberInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }

        public Input addOrderedListInput(String name, String label, FormInfo.Section.Field data) {
            OrderedListInput input = new OrderedListInput();
            input.name = name;
            input.label = label;

            if(data.options != null) {
                for(int i = 0; i < data.options.length; i++) {
                    FormInfo.Section.Field.Option optiondata = data.options[i];

                    OrderedListOption option = new OrderedListOption();
                    option.label = optiondata.label;
                    option.value = optiondata.value;
                    input.options.add(option);
                }
            }

            inputs.add(input);
            return input;
        }

        public Input addDateInput(String name, String label, String hint) {
            Input i = this.addDateInput(name, label);
            i.hint = hint;

            return i;
        }

        public Input addDateInput(String name, String label) {
            DateInput input = new DateInput();
            input.name = name;
            input.label = label;

            inputs.add(input);
            return input;
        }
    }

    public class StopReason_t {
        public long local_id;

        public int id;
        public String reason;
        public boolean reschedule;
    }

    public int id;
    public String title;
    public String description;
    public Date startTime;
    public Date endTime;
    public int quota;
    public int maxReschedules;
    public Vector<UPPSSubmission> submissions;
    public Vector<Page> pages;
    public Vector<Integer> users;
    public Vector<StopReason_t> stopReasons;
    public Hashtable<Integer, Integer> quotas;
    public boolean canStopFilling = true;
    public String etag;
    public boolean allowSubmissionTransfer = false;
    public boolean allowNewSubmissions;
    public boolean undefinedMode;
    public int assignmentId;
    public Date updated_at;

    public UPPSForm(LinkedHashMap formdata) {
        this.submissions = new Vector<UPPSSubmission>();
        this.pages = new Vector<Page>();
        this.users = new Vector<Integer>();
        this.stopReasons = new Vector<StopReason_t>();
        this.quotas = new Hashtable<Integer, Integer>();

        update(formdata);
    }

    public UPPSForm(FormInfo formdata) {
        this.submissions = new Vector<UPPSSubmission>();
        this.pages = new Vector<Page>();
        this.users = new Vector<Integer>();
        this.stopReasons = new Vector<StopReason_t>();
        this.quotas = new Hashtable<Integer, Integer>();

        parse(formdata);
    }

    public int getInputCount() {
        int count = 0;
        for(Page page : this.pages) {
            count += page.inputs.size();
        }
        return count;
    }

    public void parse(FormInfo formdata) {
        this.maxReschedules = formdata.max_reschedules;

        this.id = formdata.id;
        this.title = formdata.name;
        this.description = formdata.subtitle;
        this.allowSubmissionTransfer = formdata.allow_transfer;
        this.allowNewSubmissions = formdata.allow_new_submissions;
        this.undefinedMode = formdata.undefined_mode;
        try {
            this.updated_at = DateParser.parse(formdata.updated_at);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if(formdata.pub_start != null)
            try {
                this.startTime = DateParser.parse(formdata.pub_start);
            } catch (ParseException ex) { }

        if(formdata.pub_end != null)
            try {
                this.endTime = DateParser.parse(formdata.pub_end);
            } catch (ParseException ex) { }

        this.quota = formdata.quota;


        this.maxReschedules = formdata.max_reschedules;

        this.stopReasons.clear();
        for(FormInfo.StopReason reasondata : formdata.stop_reasons) {
            StopReason_t reason = new StopReason_t();
            reason.id = reasondata.id;
            reason.reason = reasondata.reason;
            reason.reschedule = reasondata.reschedule;
            stopReasons.add(reason);
        }

        this.pages.clear();
        for(int i = 0; i < formdata.sections.length; i++) {
            FormInfo.Section section = formdata.sections[i];
            Page page = new Page();
            page.id = section.id;
            page.order = section.order;
            page.title = section.name;
            for(int j = 0; j < section.fields.length; j++) {
                FormInfo.Section.Field field = section.fields[j];
                page.addInput(Integer.toString(field.id), field);
            }
            pages.add(page);
        }
    }

    public void update(LinkedHashMap formdata) {
        Integer id = (Integer)formdata.get("id");
        String name = (String)formdata.get("name");
        String subtitle = (String)formdata.get("subtitle");
        Boolean allow_transfer = (Boolean)formdata.get("allow_transfer");
        Boolean allow_new_submissions = (Boolean)formdata.get("allow_new_submissions");
        Boolean undefined_mode = (Boolean)formdata.get("undefined_mode");
        String pub_start = (String)formdata.get("pub_start");
        String pub_end = (String)formdata.get("pub_end");
        Integer quota = (Integer)formdata.get("quota");
        Integer max_reschedules = (Integer)formdata.get("max_reschedules");
        ArrayList<LinkedHashMap> stop_reasons = (ArrayList<LinkedHashMap>)formdata.get("stop_reasons");
        ArrayList<LinkedHashMap> sections = (ArrayList<LinkedHashMap>)formdata.get("sections");
        String updated_at = (String)formdata.get("updated_at");

        if(id != null)
            this.id = id;

        if(name != null)
            this.title = name;
        if(subtitle != null)
            this.description = subtitle;
        else
            this.description = null;

        // These fields are unpredictable
        if(allow_transfer != null)
            this.allowSubmissionTransfer = allow_transfer;
        if(allow_new_submissions != null)
            this.allowNewSubmissions = allow_new_submissions;
        if(undefined_mode != null)
            this.undefinedMode = undefined_mode;

        if(updated_at != null) {
            try {
                this.updated_at = DateParser.parse(updated_at);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(pub_start != null)
            try {
                this.startTime = DateParser.parse(pub_start);
            } catch (ParseException ex) { }
        else
            this.startTime = null;

        if(pub_end != null)
            try {
                this.endTime = DateParser.parse(pub_end);
            } catch (ParseException ex) { }
        else
            this.endTime = null;

        if(quota != null)
            this.quota = quota;
        else
            this.quota = 0;

        if(max_reschedules != null)
            this.maxReschedules = max_reschedules;
        else
            this.maxReschedules = 0;

        if(stop_reasons != null) {
            this.stopReasons.clear();
            for(LinkedHashMap reasondata : stop_reasons) {
                Integer reasonId = (Integer)reasondata.get("id");
                String reasonReason = (String)reasondata.get("reason");
                Boolean reasonReschedule = (Boolean)reasondata.get("reschedule");

                StopReason_t reason = new StopReason_t();
                if(reasonId != null)
                    reason.id = reasonId;
                if(reasonReason != null)
                    reason.reason = reasonReason;
                if(reasonReschedule != null)
                    reason.reschedule = reasonReschedule;

                stopReasons.add(reason);
            }
        }

        if(sections != null) {
            this.pages.clear();
            for(LinkedHashMap section : sections) {
                Integer pageid = (Integer)section.get("id");
                Integer pageorder = (Integer)section.get("order");
                String pagetitle = (String)section.get("name");
                ArrayList<LinkedHashMap> fields = (ArrayList<LinkedHashMap>)section.get("fields");

                Page page = new Page();
                page.id = pageid;
                if(pageorder != null)
                    page.order = pageorder;
                page.title = pagetitle;
                for(LinkedHashMap field : fields) {
                    Integer fieldid = (Integer)field.get("id");
                    page.addInput(fieldid.toString(), field);
                }
                pages.add(page);
            }
        }
    }

    public UPPSForm() {
        this.submissions = new Vector<UPPSSubmission>();
        this.pages = new Vector<Page>();
        this.users = new Vector<Integer>();
        this.stopReasons = new Vector<StopReason_t>();
        this.quotas = new Hashtable<Integer, Integer>();
    }

    public int getQuota() {
        if(quota > 0)
            return quota;

        int sum = 0;
        for(Integer value : this.quotas.values()) {
            sum += value;
        }

        return sum;
    }

    public int getRemainingCount() {
        if(getQuota() <= 0)
            return 0;

        return getQuota() - getStateCountExcept(UPPSSubmission.State.New);
   }

    public int getStateCount(int userId, UPPSSubmission.State state) {
        if(userId == 0)
            return getStateCount(state);

        int count = 0;
        Object[] subs = submissions.toArray();
        for(Object sub : subs) {
            UPPSSubmission submission = (UPPSSubmission)sub;
            if(submission.state == state && submission.getUserId() == userId)
                count++;
        }

        return count;
    }

    public int getStateCount(UPPSSubmission.State state) {
        int count = 0;
        Object[] subs = submissions.toArray();
        for(Object sub : subs) {
            UPPSSubmission submission = (UPPSSubmission)sub;
            if(submission.state == state)
                count++;
        }

        return count;
    }

    public int getStateCountExcept(int userId, UPPSSubmission.State state) {
        if(userId == 0)
            return getStateCountExcept(state);

        int count = 0;
        for(UPPSSubmission submission : submissions) {
            if(submission.state != state && submission.getUserId() == userId)
                count++;
        }

        return count;
    }

    public int getStateCountExcept(UPPSSubmission.State state) {
        int count = 0;
        for(UPPSSubmission submission : submissions) {
            if(submission.state != state)
                count++;
        }

        return count;
    }

    public int getUserQuota(int userId) {
        return this.quotas.containsKey(userId) ? this.quotas.get(userId) : 0;
    }

    public int getUserRemainingCount(int userId) {
        int quota = this.quotas.containsKey(userId) ? this.quotas.get(userId) : 0;
        if(quota <= 0)
            return 0;

        return quota - getStateCountExcept(userId, UPPSSubmission.State.New);
    }

    public StopReason_t getStopReason(int id) {
        for(StopReason_t reason : this.stopReasons) {
            if(reason.id == id)
                return reason;
        }
        return null;
    }

    public boolean hasIdentifierField() {
        Object[] $pages = pages.toArray();
        for(int i = 0; i < $pages.length; i++) {
            Page page = (Page)$pages[i];
            Object[] $inputs = page.inputs.toArray();
            for(int j = 0; j < $inputs.length; j++) {
                Input input = (Input)$inputs[j];
                if(input.isIdentifier)
                    return true;
            }
        }

        return false;
    }

    public Input getIdentifierField() {
        Object[] $pages = pages.toArray();
        for(int i = 0; i < $pages.length; i++) {
            Page page = (Page)$pages[i];
            Object[] $inputs = page.inputs.toArray();
            for(int j = 0; j < $inputs.length; j++) {
                Input input = (Input)$inputs[j];
                if(input.isIdentifier)
                    return input;
            }
        }

        return null;
    }

    public boolean areAllUsersLoaded() {
        for(Integer userId : users) {
            if(UPPSCache.getUser(userId) == null)
                return false;
        }
        return true;
    }

    public String[] getValidStopReasons() {
        String[] result = new String[stopReasons.size()];
        int i = 0;
        for(StopReason_t reason : stopReasons) {
            result[i++] = reason.reason;
        }

        return result;
    }

    public StopReason_t addStopReason(int id, String reason, boolean reschedule) {
        StopReason_t r = new StopReason_t();
        r.id = id;
        r.reason = reason;
        r.reschedule = reschedule;

        stopReasons.add(r);
        return r;
    }

    public int getInputPageIndex(String name) {
        int i = 0;
        for(Page page : this.pages) {
            for(Input input : page.inputs) {
                if(input.name.equals(name))
                    return i;
            }
            i++;
        }

        return -1;
    }

    public Input getInput(String name) {
        for(int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);

            Object[] _inputs = page.inputs.toArray();
            for(int j = 0; j < _inputs.length; j++) {
                Input input = (Input)_inputs[j];
                if(input.name.equals(name))
                    return input;
            }
        }

        return null;
    }

    public int getRemainingDays() {
        if(endTime == null)
            return 0;

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(endTime);

        float remaining = (((float)cal1.getTimeInMillis() - (float)Calendar.getInstance().getTimeInMillis()) / 1000.0f / 60.0f / 60.0f / 24.0f);
        if(remaining < 0)
            return 0;

        return Math.round(remaining);
    }

    public int getFreeSubmissionId() {
        return (submissions.size() + 1) | 0x80000000; // local mask
    }

    public boolean hasPendingSubmission() {
        boolean hasPendingSubmission = false;
        for(UPPSSubmission submission : submissions) {
            if(submission.state == UPPSSubmission.State.NotSent) {
                hasPendingSubmission = true;
                break;
            }
        }

        return hasPendingSubmission;
    }

    public UPPSSubmission getSubmission(int id) {
        for(UPPSSubmission submission : this.submissions) {
            if(submission.id == id) {
                return submission;
            }
        }

        return null;
    }

    public Page createPage(String name) {
        Page page = new Page();
        page.title = name;
        page.inputs = new Vector<Input>();

        pages.add(page);
        return page;
    }

    public UPPSSubmission createFakeSubmission() {
        UPPSSubmission submission = new UPPSSubmission(this);
        submission.id = 1;
        submission.state = UPPSSubmission.State.WaitingForApproval;
        submission.started_at = Calendar.getInstance().getTime();

        return submission;
    }
}
