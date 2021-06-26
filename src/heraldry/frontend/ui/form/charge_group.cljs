(ns heraldry.frontend.ui.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.coat-of-arms.default :as default]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:type
                 :origin
                 :spacing
                 :stretch
                 :strip-angle
                 :num-slots
                 :radius
                 :arc-angle
                 :start-angle
                 :arc-stretch
                 :rotate-charges?]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.component/charge-group [path component-data]
  {:title (str "Charge group of " (if (-> component-data :charges count (= 1))
                                    (charge/title (-> component-data :charges first))
                                    "various"))
   :buttons [{:icon "fas fa-plus"
              :menu [{:title "Charge"
                      :handler #(state/dispatch-on-event % [:add-element (conj path :charges) default/charge])}]}]
   :nodes (concat (->> component-data
                       :charges
                       count
                       range
                       reverse
                       (map (fn [idx]
                              (let [charge-path (conj path :charges idx)]
                                {:path charge-path
                                 :buttons [{:icon "fas fa-chevron-down"
                                            :disabled? (zero? idx)
                                            :tooltip "move down"
                                            :handler #(state/dispatch-on-event % [:move-charge-group-charge-down charge-path])}
                                           {:icon "fas fa-chevron-up"
                                            :disabled? (-> component-data :charges count dec (= idx))
                                            :tooltip "move up"
                                            :handler #(state/dispatch-on-event % [:move-charge-group-charge-up charge-path])}
                                           {:icon "far fa-trash-alt"
                                            :disabled? (-> component-data :charges count (= 1))
                                            :tooltip "remove"
                                            :handler #(state/dispatch-on-event
                                                       % [:remove-charge-group-charge charge-path])}]})))
                       vec))})

(defmethod interface/component-form-data :heraldry.component/charge-group [component-data]
  {:form form
   :form-args {:options (charge-group-options/options component-data)}})
