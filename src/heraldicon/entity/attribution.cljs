(ns heraldicon.entity.attribution
  (:require
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]))

(def license-choices
  [[:string.attribution.license-choice/none :none]
   [:string.attribution.license-choice/cc-attribution :cc-attribution]
   [:string.attribution.license-choice/cc-attribution-share-alike :cc-attribution-share-alike]
   [:string.attribution.license-choice/cc-attribution-non-commercial-share-alike :cc-attribution-non-commercial-share-alike]
   [:string.attribution.license-choice/public-domain :public-domain]])

(def license-map
  (util/choices->map license-choices))

(def cc-license-version-choices
  [["4.0 (default)" :v4]
   ["3.0" :v3]
   ["2.5" :v2.5]
   ["2.0" :v2]
   ["1.0" :v1]])

(def cc-license-version-map
  (util/choices->map cc-license-version-choices))

(def nature-choices
  [[:string.attribution.nature-choice/own-work :own-work]
   [:string.attribution.nature-choice/derivative :derivative]])

(def nature-map
  (util/choices->map nature-choices))

(defn cc-license? [license]
  (#{:cc-attribution
     :cc-attribution-share-alike
     :cc-attribution-non-commercial-share-alike} license))

(defn cc-version-string [version]
  (case version
    :v4 "4.0"
    :v3 "3.0"
    :v2.5 "2.5"
    :v2 "2.0"
    :v1 "1.0"
    "4.0"))

(defn license-url [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution (str "https://creativecommons.org/licenses/by/" version)
        :cc-attribution-share-alike (str "https://creativecommons.org/licenses/by-sa/" version)
        :cc-attribution-non-commercial-share-alike (str "https://creativecommons.org/licenses/by-nc-sa/" version)
        :public-domain "https://creativecommons.org/publicdomain/mark/1.0/"
        "private"))))

(defn license-display-name [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution (str "CC BY " version)
        :cc-attribution-share-alike (str "CC BY-SA " version)
        :cc-attribution-non-commercial-share-alike (str "CC BY-NC-SA " version)
        :public-domain :string.attribution.license-display/public-domain
        :string.attribution.license-display/none))))

(defn license-compatible? [license source-license]
  (let [compatible-licenses
        (case (or source-license :none)
          :none #{:none}
          :public-domain #{:none
                           :public-domain
                           :cc-attribution
                           :cc-attribution-share-alike
                           :cc-attribution-non-commercial-share-alike}
          :cc-attribution #{:none
                            :cc-attribution
                            :cc-attribution-share-alike
                            :cc-attribution-non-commercial-share-alike:none}
          :cc-attribution-share-alike #{:none
                                        :cc-attribution-share-alike
                                        :cc-attribution-non-commercial-share-alike}
          :cc-attribution-non-commercial-share-alike #{:none
                                                       :cc-attribution-non-commercial-share-alike})]

    (compatible-licenses (or license :none))))

(defn options [context]
  (-> {:license {:type :choice
                 :choices license-choices
                 :default :none
                 :ui {:label :string.attribution/license}}
       :nature {:type :choice
                :choices nature-choices
                :default :own-work
                :ui {:form-type :radio-select}}

       :ui {:label :string.attribution/title
            :form-type :attribution}}

      (cond->
        (-> context
            (c/++ :license)
            interface/get-raw-data
            cc-license?) (assoc :license-version {:type :choice
                                                  :choices cc-license-version-choices
                                                  :default :v4
                                                  :ui {:label :string.attribution/license-version}})

        (-> context
            (c/++ :nature)
            interface/get-raw-data
            (= :derivative)) (merge {:source-license {:type :choice
                                                      :choices license-choices
                                                      :default :none
                                                      :ui {:label :string.attribution/source-license}}

                                     :source-name {:type :text
                                                   :default ""
                                                   :ui {:label :string.attribution/source-name}}

                                     :source-link {:type :text
                                                   :default ""
                                                   :ui {:label :string.attribution/source-link}}

                                     :source-creator-name {:type :text
                                                           :default ""
                                                           :ui {:label :string.attribution/creator-name}}

                                     :source-creator-link {:type :text
                                                           :default ""
                                                           :ui {:label :string.attribution/creator-link}}})

        (-> context
            (c/++ :source-license)
            interface/get-raw-data
            cc-license?) (assoc :source-license-version {:type :choice
                                                         :choices cc-license-version-choices
                                                         :default :v4
                                                         :ui {:label :string.attribution/license-version}}))))

(defn full-url [context base]
  (when-let [object-id (interface/get-raw-data (c/++ context :id))]
    (let [version (interface/get-raw-data (c/++ context :version))
          version (if (zero? version)
                    (interface/get-raw-data (c/++ context :latest-version))
                    version)]
      (str (config/get :heraldicon-url) base (util/id-for-url object-id) "/" version))))

(defn full-url-for-arms [context]
  (full-url context "/arms/"))

(defn full-url-for-collection [context]
  (full-url context "/collection/"))

(defn full-url-for-charge [context]
  (full-url context "/charges/"))

(defn full-url-for-ribbon [context]
  (full-url context "/ribbons/"))

(defn full-url-for-username [username]
  (str (config/get :heraldicon-url) "/users/" username))
