(ns heraldry.frontend.form.geometry
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.state]
            [heraldry.frontend.util :as util]))

(defn form [path options & {:keys [current]}]
  (let [changes (filter some? [(when (and (:size current)
                                          (:size options)) "resized")
                               (when (and (:stretch current)
                                          (:stretch options)) "stretched")
                               (when (and (:rotation current)
                                          (:rotation options)) "rotated")
                               (when (and (:mirrored? current)
                                          (:mirrored? options)) "mirrored")
                               (when (and (:reversed? current)
                                          (:reversed? options)) "reversed")])
        current-display (-> (if (-> changes count (> 0))
                              (util/combine ", " changes)
                              "default")
                            util/upper-case-first)]
    [:div.setting
     [:label "Geometry"]
     " "
     [element/submenu path "Geometry" current-display {}
      [:div.settings
       (when (:size options)
         [element/range-input-with-checkbox (conj path :size) "Size"
          (-> options :size :min)
          (-> options :size :max)
          :default (options/get-value (:size current) (:size options))
          :display-function #(str % "%")])
       (when (:stretch options)
         [element/range-input-with-checkbox (conj path :stretch) "Stretch"
          (-> options :stretch :min)
          (-> options :stretch :max)
          :step 0.01
          :default (options/get-value (:stretch current) (:stretch options))])
       (when (:rotation options)
         [element/range-input-with-checkbox (conj path :rotation) "Rotation"
          (-> options :rotation :min)
          (-> options :rotation :max)
          :step 5
          :default (options/get-value (:rotation current) (:rotation options))])
       (when (:mirrored? options)
         [element/checkbox (conj path :mirrored?) "Mirrored"])
       (when (:reversed? options)
         [element/checkbox (conj path :reversed?) "Reversed"])]]]))
