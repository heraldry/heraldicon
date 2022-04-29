(ns heraldicon.coat-of-arms.charge.type.escutcheon
  (:require
   [heraldicon.coat-of-arms.charge.interface :as charge.interface]
   [heraldicon.coat-of-arms.charge.shared :as charge.shared]
   [heraldicon.coat-of-arms.escutcheon :as escutcheon]
   [heraldicon.coat-of-arms.field.environment :as environment]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.svg.path :as path]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/escutcheon)

(defmethod charge.interface/display-name charge-type [_] :string.render-options/escutcheon)

(defmethod interface/options charge-type [context]
  (let [escutcheon-option {:type :choice
                           :choices (-> escutcheon/choices
                                        vec
                                        (assoc-in [0 0] :string.escutcheon.type/root))
                           :default :none
                           :ui {:label :string.render-options/escutcheon
                                :form-type :escutcheon-select}}
        escutcheon (-> context (c/++ :escutcheon) interface/get-raw-data
                       (or (-> escutcheon-option :choices first second)))]
    (-> (charge.shared/options context)
        (assoc-in [:geometry :size :default] 30)
        (assoc :escutcheon escutcheon-option)
        (cond->
          (= escutcheon :flag) (merge escutcheon/flag-options)))))

(defmethod charge.interface/render-charge charge-type
  [context]
  (let [escutcheon (interface/get-sanitized-data (c/++ context :escutcheon))]
    (charge.shared/make-charge
     context
     :width
     (fn [width]
       (let [env (environment/transform-to-width
                  (if (= escutcheon :none)
                    (escutcheon/field
                     (interface/render-option :escutcheon context)
                     (interface/render-option :flag-width context)
                     (interface/render-option :flag-height context)
                     (interface/render-option :flag-swallow-tail context)
                     (interface/render-option :flag-tail-point-height context)
                     (interface/render-option :flag-tail-tongue context))
                    (escutcheon/field
                     escutcheon
                     (interface/get-sanitized-data (c/++ context :flag-width))
                     (interface/get-sanitized-data (c/++ context :flag-height))
                     (interface/get-sanitized-data (c/++ context :flag-swallow-tail))
                     (interface/get-sanitized-data (c/++ context :flag-tail-point-height))
                     (interface/get-sanitized-data (c/++ context :flag-tail-tongue))))
                  width)
             env-fess (-> env :points :fess)
             offset (v/mul env-fess -1)]
         {:shape {:paths (into []
                               (map #(-> %
                                         path/parse-path
                                         (path/translate (:x offset) (:y offset))
                                         path/to-svg))
                               (-> env :shape :paths))}
          :charge-top-left offset
          :charge-width (:width env)
          :charge-height (:height env)})))))
