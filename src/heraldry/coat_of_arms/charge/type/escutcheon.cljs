(ns heraldry.coat-of-arms.charge.type.escutcheon
  (:require [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Escutcheon"
   :value :escutcheon}
  [charge parent environment {:keys [root-escutcheon] :as context}]
  (let [{:keys [escutcheon]} (options/sanitize charge (charge-options/options charge))]
    (charge-shared/make-charge
     charge parent environment context
     :width
     (fn [width]
       (let [env (field-environment/transform-to-width
                  (escutcheon/field (if (= escutcheon :none)
                                      root-escutcheon
                                      escutcheon)) width)
             env-fess (-> env :points :fess)
             offset (v/* env-fess -1)]
         {:shape (svg/translate (:shape env)
                                (:x offset)
                                (:y offset))
          :charge-top-left offset
          :charge-width (:width env)
          :charge-height (:height env)})))))
