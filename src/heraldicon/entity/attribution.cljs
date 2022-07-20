(ns heraldicon.entity.attribution
  (:require
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.entity.id :as id]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(def ^:private license-choices
  [[:string.attribution.license-choice/none :none]
   [:string.attribution.license-choice/cc-attribution :cc-attribution]
   [:string.attribution.license-choice/cc-attribution-share-alike :cc-attribution-share-alike]
   [:string.attribution.license-choice/cc-attribution-non-commercial-share-alike :cc-attribution-non-commercial-share-alike]
   [:string.attribution.license-choice/public-domain :public-domain]])

(def license-map
  (options/choices->map license-choices))

(def cc-license-version-choices
  [["4.0 (default)" :v4]
   ["3.0" :v3]
   ["2.5" :v2.5]
   ["2.0" :v2]
   ["1.0" :v1]])

(def cc-license-version-map
  (options/choices->map cc-license-version-choices))

(def nature-choices
  [[:string.attribution.nature-choice/own-work :own-work]
   [:string.attribution.nature-choice/derivative :derivative]])

(def nature-map
  (options/choices->map nature-choices))

(defn cc-license? [license]
  (#{:cc-attribution
     :cc-attribution-share-alike
     :cc-attribution-non-commercial-share-alike} license))

(defn- cc-version-string [version]
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
                            :cc-attribution-non-commercial-share-alike}
          :cc-attribution-share-alike #{:none
                                        :cc-attribution-share-alike}
          :cc-attribution-non-commercial-share-alike #{:none
                                                       :cc-attribution-non-commercial-share-alike})]

    (compatible-licenses (or license :none))))

(defn options [context]
  (cond-> {:license {:type :option.type/choice
                     :choices license-choices
                     :default :none
                     :ui/label :string.attribution/license}
           :nature {:type :option.type/choice
                    :choices nature-choices
                    :default :own-work
                    :ui/element :ui.element/radio-select}

           :ui/label :string.attribution/title
           :ui/element :ui.element/attribution}
    (-> context
        (c/++ :license)
        interface/get-raw-data
        cc-license?) (assoc :license-version {:type :option.type/choice
                                              :choices cc-license-version-choices
                                              :default :v4
                                              :ui/label :string.attribution/license-version})

    (-> context
        (c/++ :nature)
        interface/get-raw-data
        (= :derivative)) (merge {:source-license {:type :option.type/choice
                                                  :choices license-choices
                                                  :default :none
                                                  :ui/label :string.attribution/source-license}

                                 :source-name {:type :option.type/text
                                               :default ""
                                               :ui/label :string.attribution/source-name}

                                 :source-link {:type :option.type/text
                                               :default ""
                                               :ui/label :string.attribution/source-link}

                                 :source-creator-name {:type :option.type/text
                                                       :default ""
                                                       :ui/label :string.attribution/creator-name}

                                 :source-creator-link {:type :option.type/text
                                                       :default ""
                                                       :ui/label :string.attribution/creator-link}
                                 :source-modification {:type :option.type/text
                                                       :default ""
                                                       :ui/label :string.attribution/source-modification}})

    (-> context
        (c/++ :source-license)
        interface/get-raw-data
        cc-license?) (assoc :source-license-version {:type :option.type/choice
                                                     :choices cc-license-version-choices
                                                     :default :v4
                                                     :ui/label :string.attribution/license-version})))

(defn- full-url-raw [base entity-id version]
  (str (config/get :heraldicon-url) base (id/for-url entity-id) "/" version))

(defn- full-url [context base]
  (when-let [entity-id (interface/get-raw-data (c/++ context :id))]
    (let [version (interface/get-raw-data (c/++ context :version))]
      (full-url-raw base entity-id version))))

(defn full-url-for-arms [context]
  (full-url context "/arms/"))

(defn full-url-for-collection [context]
  (full-url context "/collections/"))

(defn full-url-for-charge [context]
  (full-url context "/charges/"))

(defn full-url-for-ribbon [context]
  (full-url context "/ribbons/"))

(defn full-url-for-username [username]
  (str (config/get :heraldicon-url) "/users/" username))

(defn full-url-for-entity [entity-id version]
  (full-url-raw
   (case (id/type-from-id entity-id)
     :heraldicon.entity.type/arms "/arms/"
     :heraldicon.entity.type/charge "/charges/"
     :heraldicon.entity.type/ribbon "/ribbons/"
     :heraldicon.entity.type/collection "/collections/")
   entity-id version))
