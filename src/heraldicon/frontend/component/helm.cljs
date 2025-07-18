(ns heraldicon.frontend.component.helm
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(rf/reg-sub ::status
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :components)]))

  (fn [components [_ _path]]
    {:helmet? (->> components
                   (filter (fn [component]
                             (-> component
                                 :function
                                 (= :heraldry.charge.function/helmet))))
                   seq)
     :torse? (->> components
                  (filter (fn [component]
                            (-> component
                                :function
                                (= :heraldry.charge.function/torse))))
                  seq)}))

(defmethod component/node :heraldry/helm [{:keys [path] :as context}]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [::status path])
        components-context (c/++ context :components)
        num-helms (interface/get-list-size (c/-- context))
        num-components (interface/get-list-size components-context)
        add-menu (cond-> []
                   (not helmet?) (conj {:title :string.entity/helmet
                                        :handler #(rf/dispatch [::component.element/add components-context default/helmet])})
                   (not torse?) (conj {:title :string.entity/torse
                                       :handler #(rf/dispatch [::component.element/add components-context default/torse])})
                   true (conj {:title :string.entity/crest-charge
                               :handler #(rf/dispatch [::component.element/add components-context default/crest-charge])})
                   true (conj {:title :string.entity/crest-charge-group
                               :handler #(rf/dispatch [::component.element/add components-context default/crest-charge-group])}))]

    {:title (string/str-tr (when (> num-helms 1)
                             (str (inc (last path)) ". ")) :string.entity/helm)
     :selectable? false
     :buttons (cond-> [{:icon "far fa-clone"
                        :title :string.button/duplicate
                        :handler #(rf/dispatch [::component.element/duplicate context])}]
                (seq add-menu)
                (conj {:icon "fas fa-plus"
                       :title :string.button/add
                       :menu add-menu}))
     :draggable? true
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn
     :nodes (->> (range num-components)
                 (map (fn [idx]
                        (let [component-context (c/++ components-context idx)
                              removable? @(rf/subscribe [::component.element/removable? component-context])]
                          {:context component-context
                           :buttons (when removable?
                                      [{:icon "far fa-trash-alt"
                                        :remove? true
                                        :title :string.tooltip/remove
                                        :handler #(rf/dispatch [::component.element/remove component-context])}])})))
                 vec)}))

(defmethod component/form :heraldry/helm [_context]
  nil)

(defmethod interface/render-shape :heraldry/helm [_context]
  nil)
