(ns heraldry.frontend.form.attribution
  (:require [heraldry.frontend.form.element :as element]
            [heraldry.license :as license]
            [re-frame.core :as rf]))

(defn form [db-path]
  (let [license-nature @(rf/subscribe [:get (conj db-path :nature)])
        current-license @(rf/subscribe [:get (conj db-path :license)])
        current-source-license @(rf/subscribe [:get (conj db-path :source-license)])]
    [element/component db-path :attribution "Attribution / License" nil
     [element/select (conj db-path :license) "License"
      (assoc-in
       license/license-choices
       [0 0]
       "None (no sharing)")]
     (when (license/cc-license? current-license)
       [element/select (conj db-path :license-version) "License version"
        license/cc-license-version-choices
        :default :v4])
     [element/radio-select (conj db-path :nature) license/nature-choices
      :default :own-work]
     (when (= license-nature :derivative)
       [:<>
        [element/select (conj db-path :source-license) "Source license"
         license/license-choices]
        (when (license/cc-license? current-source-license)
          [element/select (conj db-path :source-license-version) "License version"
           license/cc-license-version-choices
           :default :v4])
        [element/text-field (conj db-path :source-name) "Source name"]
        [element/text-field (conj db-path :source-link) "Source link"]
        [element/text-field (conj db-path :source-creator-name) "Creator name"]
        [element/text-field (conj db-path :source-creator-link) "Creator link"]])
     [:div {:style {:margin-bottom "1em"}} " "]]))
