(ns heraldicon.frontend.component.charge
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.element.charge-type-select :as charge-type-select]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.interface :as interface]))

(defn- form [context]
  [:<>
   (element/element (c/++ context :adapt-to-ordinaries?))

   (when-not (interface/get-raw-data (c/++ context :preview?))
     [element/element (c/++ context :type)])

   (element/elements
    context
    [:escutcheon
     :flag-aspect-ratio-preset
     :flag-height
     :flag-width
     :flag-swallow-tail
     :flag-tail-point-height
     :flag-tail-tongue
     :anchor
     :orientation
     :geometry
     :num-points
     :eccentricity
     :wavy-rays?
     :fimbriation
     :tincture
     :outline-mode
     :vertical-mask
     :manual-blazon
     :ignore-layer-separator?])])

(defn component?
  [dragged-node-path]
  (-> dragged-node-path
      drop-last
      last
      (= :components)))

(defmethod component/node :heraldry/charge [context]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge.options/title context)
   :icon (let [url (charge-type-select/choice-preview-url context)]
           {:default url
            :selected url})
   :validation (validation/validate-charge context)
   :draggable? (component? (:path context))
   :drop-options-fn drag/drop-options
   :drop-fn drag/drop-fn
   :nodes [{:context (c/++ context :field)}]})

(defmethod component/form :heraldry/charge [_context]
  form)
