(ns heraldry.frontend.ui.form.semy
  (:require
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.util :as util]))

(defn form [context]
  [:<>
   (for [option [:origin
                 :layout
                 :rectangular?
                 :manual-blazon]]
     ^{:key option} [ui-interface/form-element (c/++ context option)])])

(defmethod ui-interface/component-node-data :heraldry.component/semy [context]
  (let [charge-context (c/++ context :charge)]
    {:title (util/str-tr {:en "Semy of "
                          :de "Besät mit "}
                         (charge-options/title charge-context))
     :nodes [{:context charge-context}]}))

(defmethod ui-interface/component-form-data :heraldry.component/semy [_context]
  {:form form})
