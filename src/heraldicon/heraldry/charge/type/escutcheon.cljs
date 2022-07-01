(ns heraldicon.heraldry.charge.type.escutcheon
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(def charge-type :heraldry.charge.type/escutcheon)

(defmethod charge.interface/display-name charge-type [_] :string.render-options/escutcheon)

(defmethod charge.interface/options charge-type [context]
  (let [escutcheon-option {:type :choice
                           :choices (-> escutcheon/choices
                                        vec
                                        (assoc-in [0 0] :string.escutcheon.type/root)
                                        (assoc-in [0 1 0] :string.escutcheon.type/root))
                           :default :none
                           :ui/label :string.render-options/escutcheon
                           :ui/element :ui.element/escutcheon-select}
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
