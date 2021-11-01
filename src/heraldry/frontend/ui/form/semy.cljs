(ns heraldry.frontend.ui.form.semy
  (:require
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.frontend.ui.interface :as interface]
   [heraldry.util :as util]))

(defn form [path]
  [:<>
   (for [option [:origin
                 :layout
                 :rectangular?
                 :manual-blazon]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/semy [context]
  (let [charge-context (update context :path conj :charge)]
    {:title (util/str-tr {:en "Semy of "
                          :de "Besät mit "}
                         (charge-options/title charge-context))
     :nodes [{:context charge-context}]}))

(defmethod interface/component-form-data :heraldry.component/semy [_context]
  {:form form})
