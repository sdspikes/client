// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.net;

import android.support.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonForm;
import org.projectbuendia.client.json.JsonLocation;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Implementation of {@link Server} that sends RPC's to OpenMRS. */
public class OpenMrsServer implements Server {
    private static final Logger LOG = Logger.create();

    private final OpenMrsConnectionDetails mConnectionDetails;
    private final RequestFactory mRequestFactory;
    private final Gson mGson;

    /**
     * Constructs an interface to the OpenMRS server.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} instance for communicating with
     *                          the server
     * @param requestFactory    a {@link RequestFactory} for generating requests to OpenMRS
     * @param gson              a {@link Gson} instance for serialization/deserialization
     */
    public OpenMrsServer(
        OpenMrsConnectionDetails connectionDetails,
        RequestFactory requestFactory,
        Gson gson) {
        mConnectionDetails = connectionDetails;
        mRequestFactory = requestFactory;
        // TODO: Inject a Gson instance here.
        mGson = gson;
    }

    @Override public void logToServer(List<String> pairs) {
        // To avoid filling the server logs with big messy stack traces, let's make a dummy
        // request that succeeds.  We assume "Pulse" will always be present on the server.
        // Conveniently, extra data after ";" in the URL is included in request logs, but
        // ignored by the REST resource handler, which just returns the "Pulse" concept.
        final String urlPath = "/concepts/" + ConceptUuids.PULSE_UUID;
        List<String> params = new ArrayList<>();
        params.add("time=" + (new Date().getTime()));
        JsonUser user = App.getUserManager().getActiveUser();
        if (user != null) {
            params.add("user_id=" + user.id);
            if (user.isGuestUser()) {
                params.add("guest_user=1");
            }
        }
        for (int i = 0; i + 1 < pairs.size(); i += 2) {
            params.add(Utils.urlEncode(pairs.get(i)) + "=" + Utils.urlEncode(pairs.get(i + 1)));
        }

        LOG.i("Logging to server: %s", params);
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails, urlPath + ";" + Joiner.on(";").join(params), null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                }
            }, null);
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 0, 1));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void addPatient(
        PatientDelta patientDelta,
        final Response.Listener<JsonPatient> successListener,
        final Response.ErrorListener errorListener) {
        JSONObject json = new JSONObject();
        if (!patientDelta.toJson(json)) {
            throw new IllegalArgumentException("Unable to serialize the patient delta to JSON.");
        }

        LOG.v("Adding patient from JSON: %s", json.toString());

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/patients",
            json,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(patientFromJson(response));
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private JsonPatient patientFromJson(JSONObject object) throws JSONException {
        JsonPatient patient = mGson.fromJson(object.toString(), JsonPatient.class);

        if (!patient.sex.matches("^[MFOU]$")) {
            LOG.e("Invalid sex from server: " + patient.sex);
            patient.sex = "U";
        }
        return patient;
    }

    /**
     * Wraps an ErrorListener so as to extract an error message from the JSON
     * content of a response, if possible.
     * @param errorListener An error listener.
     * @return A new error listener that tries to pass a more meaningful message
     * to the original errorListener.
     */
    public static Response.ErrorListener wrapErrorListener(
        final Response.ErrorListener errorListener) {
        return new OpenMrsErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                String message = parseResponse(error);
                displayErrorMessage(message);
                errorListener.onErrorResponse(new VolleyError(message, error));
            }
        };
    }

    @Override public void updatePatient(
        String patientUuid,
        PatientDelta patientDelta,
        final Response.Listener<JsonPatient> successListener,
        final Response.ErrorListener errorListener) {
        JSONObject json = new JSONObject();
        if (!patientDelta.toJson(json)) {
            throw new IllegalArgumentException("Unable to serialize the patient delta to JSON.");
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/patients/" + patientUuid,
            json,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(patientFromJson(response));
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void addUser(
        final JsonNewUser user,
        final Response.Listener<JsonUser> successListener,
        final Response.ErrorListener errorListener) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_name", user.username);
            requestBody.put("given_name", user.givenName);
            requestBody.put("family_name", user.familyName);
            requestBody.put("password", user.password);

        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/users",
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(userFromJson(response));
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private JsonUser userFromJson(JSONObject object) throws JSONException {
        return new JsonUser(object.getString("user_id"), object.getString("full_name"));
    }

    @Override public void addEncounter(Patient patient,
                             Encounter encounter,
                             final Response.Listener<JsonEncounter> successListener,
                             final Response.ErrorListener errorListener) {
        JSONObject json;
        try {
            json = encounter.toJson();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Unable to serialize the encounter to JSON.", e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/encounters",
            json,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(encounterFromJson(response));
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void deleteObservation(String Uuid,
                                         final Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            Request.Method.DELETE,
            mConnectionDetails.getRestApiUrl() + "/obs/" + Uuid,
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    LOG.i("Voided observation");
                }
            },
            wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private JsonEncounter encounterFromJson(JSONObject object) throws JSONException {
        return mGson.fromJson(object.toString(), JsonEncounter.class);
    }

    @Override public void saveOrder(Order order,
                                    final Response.Listener<JsonOrder> successListener,
                                    final Response.ErrorListener errorListener) {
        JSONObject json;
        try {
            json = order.toJson();
            JsonUser user = App.getUserManager().getActiveUser();
            if (user != null) {
                json.put("orderer_uuid", user.id);
            }
        } catch (Exception e) {
            errorListener.onErrorResponse(new VolleyError("failed to serialize request", e));
            return;
        }
        LOG.v("Saving order with JSON: %s", json);

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/orders" + (order.uuid == null ? "" : "/" + order.uuid),
            json,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(
                            mGson.fromJson(response.toString(), JsonOrder.class));
                    } catch (JsonSyntaxException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void deleteOrder(String orderUuid,
                                      final Response.Listener<Void> successListener,
                                      final Response.ErrorListener errorListener) {

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            Request.Method.DELETE,
            "/orders/" + orderUuid,
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        successListener.onResponse(null);
                    } catch (JsonSyntaxException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void getPatient(final String patientId,
                           final Response.Listener<JsonPatient> successListener,
                           final Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/patients?id=" + patientId,
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results != null && results.length() > 0) {
                            successListener.onResponse(patientFromJson(results.getJSONObject(0)));
                        } else {
                            successListener.onResponse(null);
                        }
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                        errorListener.onErrorResponse(
                            new VolleyError("Failed to parse response", e));
                    }
                }
            },
            wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void updatePatientLocation(String patientId, String newLocationId) {
        // TODO: Implement or remove (currently handled by updatePatient).
    }

    @Override public void listUsers(@Nullable String searchQuery,
                          final Response.Listener<List<JsonUser>> successListener,
                          Response.ErrorListener errorListener) {
        String query = searchQuery != null ? "?q=" + Utils.urlEncode(searchQuery) : "";
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/users" + query,
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    ArrayList<JsonUser> users = new ArrayList<>();
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            users.add(userFromJson(results.getJSONObject(i)));
                        }
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                    }
                    successListener.onResponse(users);
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void addLocation(JsonLocation location,
                            final Response.Listener<JsonLocation> successListener,
                            final Response.ErrorListener errorListener) {
        JSONObject requestBody;
        try {
            if (location.uuid != null) {
                throw new IllegalArgumentException("The server sets the uuids for new locations");
            }
            if (location.parent_uuid == null) {
                throw new IllegalArgumentException("You must set a parent_uuid for a new location");
            }
            if (location.names == null || location.names.isEmpty()) {
                throw new IllegalArgumentException(
                    "You must set a name in at least one locale for a new location");
            }
            requestBody = new JSONObject(mGson.toJson(location));
        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/locations",
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    successListener.onResponse(parseLocationJson(response));
                }
            },
            errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private JsonLocation parseLocationJson(JSONObject object) {
        return mGson.fromJson(object.toString(), JsonLocation.class);
    }

    @Override public void updateLocation(JsonLocation location,
                               final Response.Listener<JsonLocation> successListener,
                               final Response.ErrorListener errorListener) {

        if (location.uuid == null) {
            throw new IllegalArgumentException("Location must be set for update " + location);
        }
        if (location.names == null || location.names.isEmpty()) {
            throw new IllegalArgumentException("New names must be set for update " + location);
        }
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(mGson.toJson(location));
        } catch (JSONException e) {
            String msg = "Failed to write patient changes to Gson: " + location.toString();
            LOG.e(e, msg);
            errorListener.onErrorResponse(new VolleyError(msg));
            return;
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/locations/" + location.uuid,
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    successListener.onResponse(parseLocationJson(response));
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            Request.Method.DELETE, "/locations/" + locationUuid,
            null,
            null,
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void listLocations(final Response.Listener<List<JsonLocation>> successListener,
                              Response.ErrorListener errorListener) {


        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails, "/locations",
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    ArrayList<JsonLocation> result = new ArrayList<>();
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JsonLocation location =
                                parseLocationJson(results.getJSONObject(i));
                            result.add(location);
                        }
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                    }
                    successListener.onResponse(result);
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void listForms(final Response.Listener<List<JsonForm>> successListener,
                          Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
            mConnectionDetails,
            "/xforms",
            null,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    ArrayList<JsonForm> forms = new ArrayList<>();
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject result = results.getJSONObject(i);
                            forms.add(mGson.fromJson(result.toString(), JsonForm.class));
                        }
                    } catch (JSONException e) {
                        LOG.e(e, "Failed to parse response");
                    }
                    successListener.onResponse(forms);
                }
            },
            wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override public void cancelPendingRequests() {
        // TODO: Implement or deprecate. The way this was implemented before, where a string
        // was the tag, is not safe. Only the class that initiated a request (and its delegates)
        // should be able to cancel that request.
    }
}
