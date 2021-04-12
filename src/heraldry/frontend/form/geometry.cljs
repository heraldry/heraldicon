(ns heraldry.frontend.form.geometry
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.state]
            [heraldry.frontend.util :as util]))

(defn form [path options & {:keys [current]}]
  (let [changes         (filter some? [(when (and (:size current)
                                                  (:size options)) "resized")
                                       (when (and (:stretch current)
                                                  (:stretch options)) "stretched")
                                       (when (and (:mirrored? current)
                                                  (:mirrored? options)) "mirrored")
                                       (when (and (:reversed? current)
                                                  (:reversed? options)) "reversed")])
        size-mode       (options/get-value (:size-mode current) (:size-mode options))
        current-display (-> (if (-> changes count (> 0))
                              (util/combine ", " changes)
                              "default")
                            util/upper-case-first)]
    [:div.setting
     [:label "Geometry"]
     " "
     [element/submenu path "Geometry" current-display {}
      [:div.settings
       (when (:size-mode options)
         [element/radio-select (conj path :size-mode) (-> options :size-mode :choices)
          :default (-> options :size-mode :default)])
       (when (:size options)
         [element/range-input-with-checkbox (conj path :size) "Size"
          (-> options :size :min)
          (-> options :size :max)
          :default (options/get-value (:size current) (:size options))
          :display-function #(str % (case size-mode
                                      :angle "°"
                                      "%"))])
       (when (:stretch options)
         [element/range-input-with-checkbox (conj path :stretch) "Stretch"
          (-> options :stretch :min)
          (-> options :stretch :max)
          :step 0.01
          :default (options/get-value (:stretch current) (:stretch options))])
       (when (:mirrored? options)
         [element/checkbox (conj path :mirrored?) "Mirrored"])
       (when (:reversed? options)
         [element/checkbox (conj path :reversed?) "Reversed"])]]]))

