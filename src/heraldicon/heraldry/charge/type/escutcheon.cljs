(ns heraldicon.heraldry.charge.type.escutcheon
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(def charge-type :heraldry.charge.type/escutcheon)

(defmethod charge.interface/display-name charge-type [_] :string.render-options/escutcheon)

(defmethod charge.interface/options charge-type [context]
  (let [escutcheon-option {:type :option.type/choice
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

(defmethod interface/properties charge-type [context]
  (let [escutcheon (interface/get-sanitized-data (c/++ context :escutcheon))
        env (if (= escutcheon :none)
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
        env-fess (-> env :points :fess)
        offset (v/mul env-fess -1)]
    (charge.shared/process-shape
     context
     {:base-shape (mapv #(-> %
                             path/parse-path
                             (path/translate (:x offset) (:y offset))
                             path/to-svg)
                        (-> env :shape :paths))
      :base-top-left offset
      :basr-width (:width env)
      :base-height (:height env)})))
