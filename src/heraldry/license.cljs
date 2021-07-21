(ns heraldry.license
  (:require [heraldry.config :as config]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(def license-choices
  [["None" :none]
   ["CC Attribution" :cc-attribution]
   ["CC Attribution-ShareAlike" :cc-attribution-share-alike]
   ["CC Attribution-NonCommercial-ShareAlike" :cc-attribution-non-commercial-share-alike]
   ["Public Domain" :public-domain]])

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
  [["Own work" :own-work]
   ["Derivative" :derivative]])

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

(defn url [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution (str "https://creativecommons.org/licenses/by/" version)
        :cc-attribution-share-alike (str "https://creativecommons.org/licenses/by-sa/" version)
        :cc-attribution-non-commercial-share-alike (str "https://creativecommons.org/licenses/by-nc-sa/" version)
        :public-domain "https://creativecommons.org/publicdomain/mark/1.0/"
        "private"))))

(defn display-name [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution (str "CC BY " version)
        :cc-attribution-share-alike (str "CC BY-SA " version)
        :cc-attribution-non-commercial-share-alike (str "CC BY-NC-SA " version)
        :public-domain "public domain"
        "none"))))

(defn compatible? [license source-license]
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

(def default-options
  {:license {:type :choice
             :choices license-choices
             :default :none
             :ui {:label "License"}}

   :license-version {:type :choice
                     :choices cc-license-version-choices
                     :default :v4
                     :ui {:label "License version"}}

   :nature {:type :choice
            :choices nature-choices
            :default :own-work
            :ui {:form-type :radio-select}}

   :source-license {:type :choice
                    :choices license-choices
                    :default :none
                    :ui {:label "Source license"}}

   :source-license-version {:type :choice
                            :choices cc-license-version-choices
                            :default :v4
                            :ui {:label "Source license version"}}

   :source-name {:type :text
                 :default ""
                 :ui {:label "Source name"}}

   :source-link {:type :text
                 :default ""
                 :ui {:label "Source link"}}

   :source-creator-name {:type :text
                         :default ""
                         :ui {:label "Creator name"}}

   :source-creator-link {:type :text
                         :default ""
                         :ui {:label "Creator link"}}
   :ui {:label "Attribution"
        :form-type :attribution}})

(defn options [attribution]
  (-> default-options
      (cond->
       (-> attribution
           :license
           cc-license?
           not) (dissoc :license-version)

       (-> attribution
           :source-license
           cc-license?
           not) (dissoc :source-license-version)

       (-> attribution
           :nature
           (not= :derivative)) (->
                                (dissoc :source-license)
                                (dissoc :source-license-version)
                                (dissoc :source-name)
                                (dissoc :source-link)
                                (dissoc :source-creator-name)
                                (dissoc :source-creator-link)))))

(defn full-url [path base context]
  (when-let [object-id (options/raw-value (conj path :id) context)]
    (let [version (options/raw-value (conj path :version) context)
          version (if (zero? version)
                    (options/raw-value (conj path :latest-version) context)
                    version)]
      (str (config/get :heraldry-url) base (util/id-for-url object-id) "/" version))))

(defn full-url-for-arms [path context]
  (full-url path "/arms/" context))

(defn full-url-for-collection [path context]
  (full-url path "/collection/" context))

(defn full-url-for-charge [path context]
  (full-url path "/charges/" context))

(defn full-url-for-username [username]
  (str (config/get :heraldry-url) "/users/" username))
