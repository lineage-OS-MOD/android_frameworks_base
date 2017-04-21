/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.service.autofill;

import static android.view.autofill.Helper.DEBUG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Response for a {@link
 * AutofillService#onFillRequest(FillRequest, android.os.CancellationSignal, FillCallback)}.
 *
 * <p>The response typically contains one or more {@link Dataset}s, each representing a set of
 * fields that can be autofilled together, and the Android system displays a dataset picker UI
 * affordance that the user must use before the {@link android.app.Activity} is filled with
 * the dataset.
 *
 * <p>For example, for a login page with username/password where the user only has one account in
 * the response could be:
 *
 * <pre class="prettyprint">
 *  new FillResponse.Builder()
 *      .add(new Dataset.Builder(createPresentation())
 *          .setValue(id1, AutofillValue.forText("homer"))
 *          .setValue(id2, AutofillValue.forText("D'OH!"))
 *          .build())
 *      .build();
 * </pre>
 *
 * <p>If the user had 2 accounts, each with its own user-provided names, the response could be:
 *
 * <pre class="prettyprint">
 *  new FillResponse.Builder()
 *      .add(new Dataset.Builder(createFirstPresentation())
 *          .setValue(id1, AutofillValue.forText("homer"))
 *          .setValue(id2, AutofillValue.forText("D'OH!"))
 *          .build())
 *      .add(new Dataset.Builder(createSecondPresentation())
 *          .setValue(id1, AutofillValue.forText("elbarto")
 *          .setValue(id2, AutofillValue.forText("cowabonga")
 *          .build())
 *      .build();
 * </pre>
 *
 * If the service is interested on saving the user-edited data back, it must set a {@link SaveInfo}
 * in the {@link FillResponse}. Typically, the {@link SaveInfo} contains the same ids as the
 * {@link Dataset}, but other combinations are possible - see {@link SaveInfo} for more details
 *
 * <p>If the service has multiple {@link Dataset}s for different sections of the activity,
 * for example, a user section for which there are two datasets followed by an address
 * section for which there are two datasets for each user user, then it should "partition"
 * the activity in sections and populate the response with just a subset of the data that would
 * fulfill the first section (the name in our example); then once the user fills the first
 * section and taps a field from the next section (the address in our example), the Android
 * system would issue another request for that section, and so on. Note that if the user
 * chooses to populate the first section with a service provided dataset, the subsequent request
 * would contain the populated values so you don't try to provide suggestions for the first
 * section but ony for the second one based on the context of what was already filled. For
 * example, the first response could be:
 *
 * <pre class="prettyprint">
 *  new FillResponse.Builder()
 *      .add(new Dataset.Builder(createFirstPresentation())
 *          .setValue(id1, AutofillValue.forText("Homer"))
 *          .setValue(id2, AutofillValue.forText("Simpson"))
 *          .build())
 *      .add(new Dataset.Builder(createSecondPresentation())
 *          .setValue(id1, AutofillValue.forText("Bart"))
 *          .setValue(id2, AutofillValue.forText("Simpson"))
 *          .build())
 *      .build();
 * </pre>
 *
 * <p>Then after the user picks the second dataset and taps the street field to
 * trigger another autofill request, the second response could be:
 *
 * <pre class="prettyprint">
 *  new FillResponse.Builder()
 *      .add(new Dataset.Builder(createThirdPresentation())
 *          .setValue(id3, AutofillValue.forText("742 Evergreen Terrace"))
 *          .setValue(id4, AutofillValue.forText("Springfield"))
 *          .build())
 *      .add(new Dataset.Builder(createFourthPresentation())
 *          .setValue(id3, AutofillValue.forText("Springfield Power Plant"))
 *          .setValue(id4, AutofillValue.forText("Springfield"))
 *          .build())
 *      .build();
 * </pre>
 *
 * <p>The service could require user authentication at the {@link FillResponse} or the
 * {@link Dataset} level, prior to autofilling an activity - see
 * {@link FillResponse.Builder#setAuthentication(AutofillId[], IntentSender, RemoteViews)} and
 * {@link Dataset.Builder#setAuthentication(IntentSender)}.
 *
 * <p>It is recommended that you encrypt only the sensitive data but leave the labels unencrypted
 * which would allow you to provide a dataset presentation views with labels and if the user
 * chooses one of them challenge the user to authenticate. For example, if the user has a
 * home and a work address the Home and Work labels could be stored unencrypted as they don't
 * have any sensitive data while the address data is in an encrypted storage. If the user
 * chooses Home, then the platform will start your authentication flow. If you encrypt all
 * data and require auth at the response level the user will have to interact with the fill
 * UI to trigger a request for the datasets (as they don't see the presentation views for the
 * possible options) which will start your auth flow and after successfully authenticating
 * the user will be presented with the Home and Work options to pick one. Hence, you have
 * flexibility how to implement your auth while storing labels non-encrypted and data
 * encrypted provides a better user experience.
 */
public final class FillResponse implements Parcelable {

    private final @Nullable ArrayList<Dataset> mDatasets;
    private final @Nullable SaveInfo mSaveInfo;
    private final @Nullable Bundle mClientState;
    private final @Nullable RemoteViews mPresentation;
    private final @Nullable IntentSender mAuthentication;
    private final @Nullable AutofillId[] mAuthenticationIds;
    private final @Nullable AutofillId[] mIgnoredIds;

    private FillResponse(@NonNull Builder builder) {
        mDatasets = builder.mDatasets;
        mSaveInfo = builder.mSaveInfo;
        mClientState = builder.mCLientState;
        mPresentation = builder.mPresentation;
        mAuthentication = builder.mAuthentication;
        mAuthenticationIds = builder.mAuthenticationIds;
        mIgnoredIds = builder.mIgnoredIds;
    }

    /** @hide */
    public @Nullable Bundle getClientState() {
        return mClientState;
    }

    /** @hide */
    public @Nullable ArrayList<Dataset> getDatasets() {
        return mDatasets;
    }

    /** @hide */
    public @Nullable SaveInfo getSaveInfo() {
        return mSaveInfo;
    }

    /** @hide */
    public @Nullable RemoteViews getPresentation() {
        return mPresentation;
    }

    /** @hide */
    public @Nullable IntentSender getAuthentication() {
        return mAuthentication;
    }

    /** @hide */
    public @Nullable AutofillId[] getAuthenticationIds() {
        return mAuthenticationIds;
    }

    /** @hide */
    public @Nullable AutofillId[] getIgnoredIds() {
        return mIgnoredIds;
    }

    /**
     * Builder for {@link FillResponse} objects. You must to provide at least
     * one dataset or set an authentication intent with a presentation view.
     */
    public static final class Builder {
        private ArrayList<Dataset> mDatasets;
        private SaveInfo mSaveInfo;
        private Bundle mCLientState;
        private RemoteViews mPresentation;
        private IntentSender mAuthentication;
        private AutofillId[] mAuthenticationIds;
        private AutofillId[] mIgnoredIds;
        private boolean mDestroyed;

        /**
         * Requires a fill response authentication before autofilling the activity with
         * any data set in this response.
         *
         * <p>This is typically useful when a user interaction is required to unlock their
         * data vault if you encrypt the data set labels and data set data. It is recommended
         * to encrypt only the sensitive data and not the data set labels which would allow
         * auth on the data set level leading to a better user experience. Note that if you
         * use sensitive data as a label, for example an email address, then it should also
         * be encrypted. The provided {@link android.app.PendingIntent intent} must be an
         * activity which implements your authentication flow. Also if you provide an auth
         * intent you also need to specify the presentation view to be shown in the fill UI
         * for the user to trigger your authentication flow.
         *
         * <p>When a user triggers autofill, the system launches the provided intent
         * whose extras will have the {@link AutofillManager#EXTRA_ASSIST_STRUCTURE screen
         * content}. Once you complete your authentication flow you should set the activity
         * result to {@link android.app.Activity#RESULT_OK} and provide the fully populated
         * {@link FillResponse response} by setting it to the {@link
         * AutofillManager#EXTRA_AUTHENTICATION_RESULT} extra.
         * For example, if you provided an empty {@link FillResponse resppnse} because the
         * user's data was locked and marked that the response needs an authentication then
         * in the response returned if authentication succeeds you need to provide all
         * available data sets some of which may need to be further authenticated, for
         * example a credit card whose CVV needs to be entered.
         *
         * <p>If you provide an authentication intent you must also provide a presentation
         * which is used to visualize visualize the response for triggering the authentication
         * flow.
         *
         * <p></><strong>Note:</strong> Do not make the provided pending intent
         * immutable by using {@link android.app.PendingIntent#FLAG_IMMUTABLE} as the
         * platform needs to fill in the authentication arguments.
         *
         * @param authentication Intent to an activity with your authentication flow.
         * @param presentation The presentation to visualize the response.
         * @param ids id of Views that when focused will display the authentication UI affordance.
         *
         * @return This builder.
         * @see android.app.PendingIntent#getIntentSender()
         */
        public @NonNull Builder setAuthentication(@NonNull AutofillId[] ids,
                @Nullable IntentSender authentication, @Nullable RemoteViews presentation) {
            throwIfDestroyed();
            // TODO(b/33197203): assert ids is not null nor empty once old version is removed
            if (authentication == null ^ presentation == null) {
                throw new IllegalArgumentException("authentication and presentation"
                        + " must be both non-null or null");
            }
            mAuthentication = authentication;
            mPresentation = presentation;
            mAuthenticationIds = ids;
            return this;
        }

        /**
         * TODO(b/33197203): will be removed once clients use the version that takes ids
         * @hide
         * @deprecated
         */
        @Deprecated
        public @NonNull Builder setAuthentication(@Nullable IntentSender authentication,
                @Nullable RemoteViews presentation) {
            return setAuthentication(null, authentication, presentation);
        }

        /**
         * Specifies views that should not trigger new
         * {@link AutofillService#onFillRequest(FillRequest, android.os.CancellationSignal,
         * FillCallback)} requests.
         *
         * <p>This is typically used when the service cannot autofill the view; for example, an
         * {@code EditText} representing a captcha.
         */
        public Builder setIgnoredIds(AutofillId...ids) {
            mIgnoredIds = ids;
            return this;
        }

        /**
         * Adds a new {@link Dataset} to this response.
         *
         * @return This builder.
         */
        public @NonNull Builder addDataset(@Nullable Dataset dataset) {
            throwIfDestroyed();
            if (dataset == null) {
                return this;
            }
            if (mDatasets == null) {
                mDatasets = new ArrayList<>();
            }
            if (!mDatasets.add(dataset)) {
                return this;
            }
            return this;
        }

        /**
         * Sets the {@link SaveInfo} associated with this response.
         *
         * <p>See {@link FillResponse} for more info.
         *
         * @return This builder.
         */
        public @NonNull Builder setSaveInfo(@NonNull SaveInfo saveInfo) {
            throwIfDestroyed();
            mSaveInfo = saveInfo;
            return this;
        }

        /**
         * @deprecated Use {@link #setClientState(Bundle)} instead.
         * @hide
         */
        @Deprecated
        public Builder setExtras(@Nullable Bundle extras) {
            throwIfDestroyed();
            mCLientState = extras;
            return this;
        }

        /**
         * Sets a {@link Bundle state} that will be passed to subsequent APIs that
         * manipulate this response. For example, they are passed to subsequent
         * calls to {@link AutofillService#onFillRequest(FillRequest, android.os.CancellationSignal,
         * FillCallback)} and {@link AutofillService#onSaveRequest(SaveRequest, SaveCallback)}.
         * You can use this to store intermediate state that is persistent across multiple
         * fill requests and the subsequent save request.
         *
         * <p>If this method is called on multiple {@link FillResponse} objects for the same
         * activity, just the latest bundle is passed back to the service.
         *
         * <p>Once a {@link AutofillService#onSaveRequest(SaveRequest, SaveCallback)
         * save request} is made the client state is cleared.
         *
         * @param clientState The custom client state.
         * @return This builder.
         */
        public Builder setClientState(@Nullable Bundle clientState) {
            throwIfDestroyed();
            mCLientState = clientState;
            return this;
        }

        /**
         * Builds a new {@link FillResponse} instance. You must provide at least
         * one dataset or some savable ids or an authentication with a presentation
         * view.
         *
         * @return A built response.
         */
        public FillResponse build() {
            throwIfDestroyed();

            if (mAuthentication == null && mDatasets == null && mSaveInfo == null) {
                throw new IllegalArgumentException("need to provide at least one DataSet or a "
                        + "SaveInfo or an authentication with a presentation");
            }
            mDestroyed = true;
            return new FillResponse(this);
        }

        private void throwIfDestroyed() {
            if (mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    /////////////////////////////////////
    // Object "contract" methods. //
    /////////////////////////////////////
    @Override
    public String toString() {
        if (!DEBUG) return super.toString();

        return new StringBuilder(
                "FillResponse: [datasets=").append(mDatasets)
                .append(", saveInfo=").append(mSaveInfo)
                .append(", clientState=").append(mClientState != null)
                .append(", hasPresentation=").append(mPresentation != null)
                .append(", hasAuthentication=").append(mAuthentication != null)
                .append(", authenticationSize=").append(mAuthenticationIds != null
                        ? mAuthenticationIds.length : "N/A")
                .append(", ignoredIdsSize=").append(mIgnoredIds != null
                    ? mIgnoredIds.length : "N/A")
                .toString();
    }

    /////////////////////////////////////
    // Parcelable "contract" methods. //
    /////////////////////////////////////

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeTypedArrayList(mDatasets, flags);
        parcel.writeParcelable(mSaveInfo, flags);
        parcel.writeParcelable(mClientState, flags);
        parcel.writeParcelableArray(mAuthenticationIds, flags);
        parcel.writeParcelable(mAuthentication, flags);
        parcel.writeParcelable(mPresentation, flags);
        parcel.writeParcelableArray(mIgnoredIds, flags);
    }

    public static final Parcelable.Creator<FillResponse> CREATOR =
            new Parcelable.Creator<FillResponse>() {
        @Override
        public FillResponse createFromParcel(Parcel parcel) {
            // Always go through the builder to ensure the data ingested by
            // the system obeys the contract of the builder to avoid attacks
            // using specially crafted parcels.
            final Builder builder = new Builder();
            final ArrayList<Dataset> datasets = parcel.readTypedArrayList(null);
            final int datasetCount = (datasets != null) ? datasets.size() : 0;
            for (int i = 0; i < datasetCount; i++) {
                builder.addDataset(datasets.get(i));
            }
            builder.setSaveInfo(parcel.readParcelable(null));
            builder.setClientState(parcel.readParcelable(null));
            builder.setAuthentication(parcel.readParcelableArray(null, AutofillId.class),
                    parcel.readParcelable(null), parcel.readParcelable(null));
            builder.setIgnoredIds(parcel.readParcelableArray(null, AutofillId.class));
            return builder.build();
        }

        @Override
        public FillResponse[] newArray(int size) {
            return new FillResponse[size];
        }
    };
}
