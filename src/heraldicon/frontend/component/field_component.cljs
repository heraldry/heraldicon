(ns heraldicon.frontend.component.field-component
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.element :as-alias component.element]
   [re-frame.core :as rf]))

(defn drop-options-fn
  [dragged-node-path drop-node-path drop-node-open?]
  (when (not= (take (count dragged-node-path) drop-node-path)
              dragged-node-path)
    (let [parent? (= (drop-last 3 dragged-node-path)
                     drop-node-path)
          siblings? (= (drop-last dragged-node-path)
                       (drop-last drop-node-path))
          component? (= (last (drop-last dragged-node-path))
                        :components)]
      (when component?
        (cond-> (cond
                  parent? #{:above :below}

                  siblings? (let [current-index (last dragged-node-path)
                                  new-index (last drop-node-path)]
                              (cond
                                (= new-index
                                   (dec current-index)) #{:above :inside}

                                (= new-index
                                   (inc current-index)) #{:below :inside}

                                :else #{:above :inside :below}))

                  :else #{:above :inside :below})
          drop-node-open? (disj :below))))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [new-index (last (:path drop-node-context))
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :inside (c/++ drop-node-context :field :components 10000)
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (rf/dispatch [::component.element/move-general
                  dragged-node-context
                  target-context])))
