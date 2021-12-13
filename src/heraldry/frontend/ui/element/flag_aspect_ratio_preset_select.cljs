(ns heraldry.frontend.ui.element.flag-aspect-ratio-preset-select
  (:require
   [clojure.string :as s]
   [heraldry.context :as c]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.select :as select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(macros/reg-event-db ::set-flag-width-and-height
  (fn [db [_ context width height]]
    (let [width-path (-> context c/-- (c/++ :flag-width) :path)
          height-path (-> context c/-- (c/++ :flag-height) :path)]
      (-> db
          (assoc-in width-path width)
          (assoc-in height-path height)))))

(defmethod ui-interface/form-element :flag-aspect-ratio-preset-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [choices (:choices option)
          label (-> option :ui :label)]
      [select/raw-select context :none label choices
       :on-change (fn [value]
                    (let [[_ height width] (-> value
                                               name
                                               (s/split "-"))]
                      (rf/dispatch [::set-flag-width-and-height
                                    context
                                    (js/parseInt width)
                                    (js/parseInt height)])))])))
