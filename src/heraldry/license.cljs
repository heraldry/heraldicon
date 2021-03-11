(ns heraldry.license
  (:require [heraldry.util :as util]))

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
    :v2 "2.0"
    :v1 "1.0"
    "4.0"))

(defn url [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution                            (str "https://creativecommons.org/licenses/by/" version)
        :cc-attribution-share-alike                (str "https://creativecommons.org/licenses/by-sa/" version)
        :cc-attribution-non-commercial-share-alike (str "https://creativecommons.org/licenses/by-nc-sa/" version)
        :public-domain                             "https://creativecommons.org/publicdomain/mark/1.0/"
        "private"))))

(defn display-name [license license-version]
  (when license
    (let [version (cc-version-string license-version)]
      (case license
        :cc-attribution                            (str "CC BY " version)
        :cc-attribution-share-alike                (str "CC BY-SA " version)
        :cc-attribution-non-commercial-share-alike (str "CC BY-NC-SA " version)
        :public-domain                             "public domain"
        "none"))))

(defn compatible? [license source-license]
  (let [compatible-licenses
        (case (or source-license :none)
          :none                                      #{:none}
          :public-domain                             #{:none
                                                       :public-domain
                                                       :cc-attribution
                                                       :cc-attribution-share-alike
                                                       :cc-attribution-non-commercial-share-alike}
          :cc-attribution                            #{:none
                                                       :cc-attribution
                                                       :cc-attribution-share-alike
                                                       :cc-attribution-non-commercial-share-alike:none}
          :cc-attribution-share-alike                #{:none
                                                       :cc-attribution-share-alike
                                                       :cc-attribution-non-commercial-share-alike}
          :cc-attribution-non-commercial-share-alike #{:none
                                                       :cc-attribution-non-commercial-share-alike})]

    (compatible-licenses (or license :none))))
