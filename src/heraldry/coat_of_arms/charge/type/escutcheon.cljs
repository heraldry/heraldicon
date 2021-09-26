(ns heraldry.coat-of-arms.charge.type.escutcheon
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.interface :as interface]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/escutcheon)

(defmethod charge-interface/display-name charge-type [_] {:en "Escutcheon / Inescutcheon"
                                                          :de "Schild / Herzschild"})

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment {:keys [root-escutcheon] :as context}]
  (let [escutcheon (interface/get-sanitized-data (conj path :escutcheon) context)]
    (charge-shared/make-charge
     path parent-path environment context
     :width
     (fn [width]
       (let [env (environment/transform-to-width
                  (escutcheon/field (if (= escutcheon :none)
                                      root-escutcheon
                                      escutcheon)) width)
             env-fess (-> env :points :fess)
             offset (v/mul env-fess -1)]
         {:shape (path/translate (:shape env)
                                 (:x offset)
                                 (:y offset))
          :charge-top-left offset
          :charge-width (:width env)
          :charge-height (:height env)})))))
