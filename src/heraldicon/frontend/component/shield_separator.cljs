(ns heraldicon.frontend.component.shield-separator
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as-alias component.element]
   [heraldicon.frontend.component.field-component :as field-component]
   [re-frame.core :as rf]))

(defn drop-options-fn
  [dragged-node-path dragged-node-type
   drop-node-path _drop-node-type
   drop-node-open?]
  (cond-> (cond
            (field-component/inside-own-subtree?
             dragged-node-path
             drop-node-path) nil

            (not (or (isa? dragged-node-type
                           :heraldry/charge)
                     (isa? dragged-node-type
                           :heraldry/charge-group)
                     (isa? dragged-node-type
                           :heraldry/shield-separator))) nil

            (field-component/sibling?
             dragged-node-path
             drop-node-path) (let [current-index (last dragged-node-path)
                                   new-index (last drop-node-path)]
                               (cond
                                 (= new-index
                                    (dec current-index)) #{:above}

                                 (= new-index
                                    (inc current-index)) #{:below}

                                 :else #{:above :below}))

            (or (isa? dragged-node-type
                      :heraldry/charge)
                (isa? dragged-node-type
                      :heraldry/charge-group)) #{:above :below}

            :else nil)
    drop-node-open? (disj :below)))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [new-index (last (:path drop-node-context))
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (rf/dispatch [::component.element/move-general
                  dragged-node-context
                  target-context])))

(defmethod component/node :heraldry/shield-separator [_context]
  {:title :string.miscellaneous/shield-layer
   :selectable? false
   :draggable? true
   :drop-options-fn drop-options-fn
   :drop-fn drop-fn})

(defmethod component/form :heraldry/shield-separator [_context]
  {})
