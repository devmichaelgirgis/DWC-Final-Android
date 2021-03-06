package fragment.companyDocumentsScreen;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import RestAPI.RestMessages;
import RestAPI.SFResponseManager;
import RestAPI.SoqlStatements;
import cloudconcept.dwc.R;
import fragment.BaseFragmentThreeSteps;
import fragmentActivity.RequestTrueCopyActivity;
import model.Contract_DWC__c;
import model.EServices_Document_Checklist__c;
import model.FormField;
import model.Receipt_Template__c;
import model.RecordType;
import model.WebForm;
import utilities.Utilities;

/**
 * Created by Abanoub Wagdy on 9/12/2015.
 */
public class RequestTrueCopyInitialPage extends Fragment {

    private RestRequest restRequest;
    RequestTrueCopyActivity activity;
    EServices_Document_Checklist__c eServices_document_checklist__c;
    String soql = "SELECT Id, Name, DeveloperName, SobjectType FROM RecordType WHERE SobjectType = 'Case' AND DeveloperName = " + "\'" + "%s" + "\'";
    String webFormId;
    Receipt_Template__c currentServiceAdministration;
    String caseType;
    private HashMap<String, Object> caseFields;
    private Map<String, String> parameters;
    LinearLayout linearAddForms;
    private WebForm webForm;
    private ArrayList<FormField> picklist;
    private String QueryBuilder = "";
    private String TrueCopyQuery = "select Id ,%s FROM eServices_Document_Checklists__r where Id=" + "\'" + "%s" + "\'";
    private String result;
    private ArrayList<Contract_DWC__c> contract_dwc__cs;

    public static Fragment newInstance(String s) {
        RequestTrueCopyInitialPage fragment = new RequestTrueCopyInitialPage();
        Bundle bundle = new Bundle();
        bundle.putString("text", s);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.true_copy_first_page, container, false);
        contract_dwc__cs = new ArrayList<>();
        InitializeViews(view);
        return view;
    }

    private void InitializeViews(View view) {
        linearAddForms = (LinearLayout) view.findViewById(R.id.linearAddForms);
        activity = (RequestTrueCopyActivity) getActivity();
        eServices_document_checklist__c = activity.geteServices_document_checklist__c();
        DoRecordTypeRequest();
    }

    private void DoRecordTypeRequest() {
        Utilities.showloadingDialog(getActivity());
        soql = String.format(soql, eServices_document_checklist__c.getEService_Administration__r().getRecord_Type_Picklist__c());
        try {
            restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);
            new ClientManager(getActivity(), SalesforceSDKManager.getInstance().getAccountType(), SalesforceSDKManager.getInstance().getLoginOptions(), SalesforceSDKManager.getInstance().shouldLogoutWhenTokenRevoked()).getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                @Override
                public void authenticatedRestClient(RestClient client) {
                    if (client == null) {
                        SalesforceSDKManager.getInstance().logout(getActivity());
                        return;
                    } else {
                        try {
                            new LeasingInfoTask(client, 0, 1).execute().get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        if (client == null) {
                            new ClientManager(getActivity(), SalesforceSDKManager.getInstance().getAccountType(), SalesforceSDKManager.getInstance().getLoginOptions(), SalesforceSDKManager.getInstance().shouldLogoutWhenTokenRevoked()).getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                                @Override
                                public void authenticatedRestClient(RestClient client) {
                                    if (client == null) {
                                        SalesforceSDKManager.getInstance().logout(getActivity());
                                        return;
                                    } else {
                                        DoYourRequest(client);
                                    }
                                }
                            });
                        } else {
                            DoYourRequest(client);
                        }
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void DoYourRequest(RestClient client) {
        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {

            @Override
            public void onSuccess(RestRequest request, RestResponse result) {

                Log.d("", result.toString());
                try {
                    RecordType recordType = SFResponseManager.parseRecordTypeResponse(result.toString());
                    requestForOriginalDocument(recordType);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception exception) {
                Utilities.showToast(getActivity(), RestMessages.getInstance().getErrorMessage());
                Utilities.dismissLoadingDialog();
                getActivity().finish();
            }
        });
    }

    private void requestForOriginalDocument(RecordType recordType) {
        webFormId = eServices_document_checklist__c.getEService_Administration__r().getNew_Edit_VF_Generator__c();
        currentServiceAdministration = eServices_document_checklist__c.getEService_Administration__r();

        caseFields = (HashMap<String, Object>) activity.getCaseFields();
        caseType = "Registration Services";
        if (recordType.getDeveloperName().toLowerCase().contains("Leasing".toLowerCase())) {
            caseType = "Leasing Services";
        }
        caseFields.put("Service_Requested__c", currentServiceAdministration.getID());
        caseFields.put("Visual_Force_Generator__c", currentServiceAdministration.getNew_Edit_VF_Generator__c());
        caseFields.put("AccountId", activity.getUser().get_contact().get_account().getID());
        caseFields.put("RecordTypeId", recordType.getId());
        caseFields.put("Status", "Draft");
        caseFields.put("Type", caseType);
        caseFields.put("Origin", "Mobile");
        activity.setCaseFields(caseFields);

        parameters = (HashMap<String, String>) activity.getParameters();
        parameters.put("accountID", activity.getUser().get_contact().get_account().getID());
        parameters.put("actName", activity.getUser().get_contact().get_account().getName());
        parameters.put("licName", activity.getUser().get_contact().get_account().get_currentLicenseNumber().getLicense_Number_Value());
        parameters.put("licID", activity.getUser().get_contact().get_account().get_currentLicenseNumber().getId());

        DoFormFieldRequest(webFormId);
        activity.setParameters(parameters);
    }

    private void DoFormFieldRequest(String webFormId) {
        String formFieldsql = SoqlStatements.getInstance().constructWebFormQuery(webFormId);
        try {
            restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), formFieldsql);
            new ClientManager(getActivity(), SalesforceSDKManager.getInstance().getAccountType(), SalesforceSDKManager.getInstance().getLoginOptions(), SalesforceSDKManager.getInstance().shouldLogoutWhenTokenRevoked()).getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                @Override
                public void authenticatedRestClient(final RestClient client) {
                    if (client == null) {
                        SalesforceSDKManager.getInstance().logout(getActivity());
                        return;
                    } else {
                        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {

                            @Override
                            public void onSuccess(RestRequest request, RestResponse result) {
                                Log.d("", result.toString());
                                webForm = SFResponseManager.parseWebFormObject(result.toString());
                                ProcessTitle(webForm);
                                activity.setWebForm(webForm);
                                picklist = new ArrayList<FormField>();
                                for (int i = 0; i < activity.getWebForm().get_formFields().size(); i++) {
                                    if (!webForm.get_formFields().get(i).getType().equals("CUSTOMTEXT") && activity.getWebForm().get_formFields().get(i).isParameter() == false && activity.getWebForm().get_formFields().get(i).isQuery() == true) {
                                        QueryBuilder += webForm.get_formFields().get(i).getTextValue() + ",";
                                    } else if (!activity.getWebForm().get_formFields().get(i).isParameter() && activity.getWebForm().get_formFields().get(i).getType().equals("PICKLIST")) {
                                        picklist.add(webForm.get_formFields().get(i));
                                    }
                                }
                                if (QueryBuilder.equals("")) {
                                    if (picklist != null && picklist.size() > 0) {
                                        new GetPickLists(client).execute(picklist);
                                    } else {
                                        Utilities.dismissLoadingDialog();
                                        Utilities.DrawFormFieldsOnLayout(getActivity(), getActivity().getApplicationContext(), linearAddForms, activity.getWebForm().get_formFields(), parameters, eServices_document_checklist__c, eServices_document_checklist__c.getEService_Administration__r());
                                    }
                                } else {
                                    QueryBuilder = QueryBuilder.substring(0, QueryBuilder.length() - 1);
                                    TrueCopyQuery = String.format(TrueCopyQuery, QueryBuilder, eServices_document_checklist__c.getId());
                                    PerformTrueCopyValues(webForm, TrueCopyQuery);
                                }
                            }

                            @Override
                            public void onError(Exception exception) {
                                Utilities.showToast(getActivity(), RestMessages.getInstance().getErrorMessage());
                                Utilities.dismissLoadingDialog();
                                getActivity().finish();
                            }
                        });
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void PerformTrueCopyValues(WebForm webForm, String trueCopyQuery) {
        try {
            restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), trueCopyQuery);
            new ClientManager(getActivity(), SalesforceSDKManager.getInstance().getAccountType(), SalesforceSDKManager.getInstance().getLoginOptions(), SalesforceSDKManager.getInstance().shouldLogoutWhenTokenRevoked()).getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                @Override
                public void authenticatedRestClient(final RestClient client) {
                    if (client == null) {
                        SalesforceSDKManager.getInstance().logout(getActivity());
                        return;
                    } else {
                        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {

                            @Override
                            public void onSuccess(RestRequest request, RestResponse result) {
                                if (picklist != null & picklist.size() > 0) {
                                    new GetPickLists(client).execute(picklist);
                                } else {
                                    Utilities.dismissLoadingDialog();
                                }
                            }

                            @Override
                            public void onError(Exception exception) {
                                Utilities.showToast(getActivity(), RestMessages.getInstance().getErrorMessage());
                                Utilities.dismissLoadingDialog();
                                getActivity().finish();
                            }
                        });
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void ProcessTitle(WebForm webForm) {
        String name = webForm.getName();
        String[] str = name.split(" ");
        String title = "";
        for (int i = 1; i < str.length; i++) {
            title += str[i] + " ";
        }
        BaseFragmentThreeSteps.setTitle(title.substring(0, title.length() - 1));
    }

    public class GetPickLists extends AsyncTask<List<FormField>, Void, Map<String, List<String>>> {

        private final RestClient client;
        String result;

        public GetPickLists(RestClient client) {
            this.client = client;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Utilities.showloadingDialog(getActivity());
        }

        @Override
        protected Map<String, List<String>> doInBackground(List<FormField>... params) {
            String attUrl = client.getClientInfo().resolveUrl("/services/apexrest/MobilePickListValuesWebService?fieldId=").toString();
            for (FormField fieldId : params[0]) {
                HttpClient tempClient = new DefaultHttpClient();
                URI theUrl = null;
                try {
                    theUrl = new URI(attUrl + fieldId.getId());
                    HttpGet getRequest = new HttpGet();

                    getRequest.setURI(theUrl);
                    getRequest.setHeader("Authorization", "Bearer " + client.getAuthToken());
                    BasicHttpParams param = new BasicHttpParams();

//                    param.setParameter("fieldId", fieldId);
                    getRequest.setParams(param);
                    HttpResponse httpResponse = null;
                    try {
                        httpResponse = tempClient.execute(getRequest);
                        StatusLine statusLine = httpResponse.getStatusLine();
                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                            HttpEntity httpEntity = httpResponse.getEntity();

                            if (httpEntity != null) {
                                result = EntityUtils.toString(httpEntity);
                                JSONObject jo = null;
                                try {
                                    jo = new JSONObject(result);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                JSONArray ja = null;
                                try {
                                    ja = jo.getJSONArray(parameters.get("auth"));
                                    fieldId.setPicklistEntries(convertJsonStringToString(ja));
                                } catch (JSONException e) {
                                    e.printStackTrace();

                                }


                            }
                        } else {
                            httpResponse.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Map<String, List<String>> aVoid) {
            super.onPostExecute(aVoid);
            Utilities.dismissLoadingDialog();

            Utilities.DrawFormFieldsOnLayout(getActivity(), getActivity().getApplicationContext(), linearAddForms, webForm.get_formFields(), parameters, eServices_document_checklist__c, eServices_document_checklist__c.geteService_Administration__r());
        }
    }

    public String convertJsonStringToString(JSONArray jsonArray) {
        String result = "";
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                result += jsonArray.getString(i);
                if (i != (jsonArray.length() - 1))
                    result += ",";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public class LeasingInfoTask extends AsyncTask<Void, Void, Void> {

        private final RestClient client;
        private int offset;
        private int limit;

        public LeasingInfoTask(RestClient client, int offset, int limit) {
            this.client = client;
            this.limit = limit;
            this.offset = offset;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String attUrl = client.getClientInfo().resolveUrl("/services/apexrest/MobileTenantContractsWebService").toString();
            attUrl += "?AccountId=" + activity.getUser().get_contact().get_account().getID() + "&LIMIT=" + limit + "&OFFSET=" + offset;
            HttpClient tempClient = new DefaultHttpClient();
            URI theUrl = null;
            try {
                theUrl = new URI(attUrl);
                HttpGet getRequest = new HttpGet();
                getRequest.setURI(theUrl);
                getRequest.setHeader("Authorization", "Bearer " + client.getAuthToken());
                HttpResponse httpResponse = null;
                try {
                    httpResponse = tempClient.execute(getRequest);
                    StatusLine statusLine = httpResponse.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        HttpEntity httpEntity = httpResponse.getEntity();
                        Log.d("response", httpEntity.toString());
                        if (httpEntity != null) {
                            result = EntityUtils.toString(httpEntity);
                        }
                    } else {
                        httpResponse.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (result != null && result.equals("[]")) {
//                tvNoEmployees.setVisibility(View.VISIBLE);
            } else {
//                tvNoEmployees.setVisibility(View.GONE);
                contract_dwc__cs.addAll(SFResponseManager.parseLeasingContractResponse2(result.toString()));
                ArrayList<Contract_DWC__c> contracts = new ArrayList<>();
                for (Contract_DWC__c contract_dwc__c : contract_dwc__cs) {
                    contracts.add(contract_dwc__c);
                }
                parameters = activity.getParameters();
                if (parameters == null) {
                    parameters = new HashMap<>();
                }
                parameters.put("tenID", contracts.get(0).getID());
                parameters.put("tenName", contracts.get(0).getName());
                activity.setParameters(parameters);
            }
        }
    }
}