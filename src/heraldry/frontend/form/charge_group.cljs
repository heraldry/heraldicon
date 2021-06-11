(ns heraldry.frontend.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.position :as position]
            [re-frame.core :as rf]))

(defn form [path & {:keys [parent-field form-for-field]}]
  (let [charge-group @(rf/subscribe [:get path])
        title (-> charge-group :type charge-group-options/type-map)
        options charge-group-options/default-options]
    [element/component path :charge-group title "Charge group"
     [element/select (conj path :type) "Type" charge-group-options/type-choices]
     (when (-> options :origin)
       [position/form (conj path :origin)
        :title "Origin"
        :options (:origin options)])
     (when (-> options :spacing)
       [element/range-input (conj path :spacing) "Spacing"
        (-> options :spacing :min)
        (-> options :spacing :max)
        :step 0.01
        :default (options/get-value (:spacing charge-group) (:spacing options))])
     (when (-> options :stretch)
       [element/range-input (conj path :stretch) "Stretch"
        (-> options :stretch :min)
        (-> options :stretch :max)
        :step 0.01
        :default (options/get-value (:stretch charge-group) (:stretch options))])]))
