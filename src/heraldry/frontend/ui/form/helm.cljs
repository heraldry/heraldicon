(ns heraldry.frontend.ui.form.helm
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(rf/reg-sub :get-helm-status
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    {:helmet? (:helmet data)
     :torse? (:torse data)
     :crest? (:crest data)}))

(rf/reg-event-db :add-helm-part
  (fn [db [_ path value]]
    (-> db
        (assoc-in path value)
        (state/ui-component-node-select path :open? true)
        submenu/ui-submenu-close-all)))

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/helm [path]
  (let [{:keys [helmet?
                torse?
                crest?]} @(rf/subscribe [:get-helm-status path])
        add-menu (cond-> []
                   (not helmet?) {:title "Helmet"
                                  :handler #(state/dispatch-on-event
                                             % [:add-helm-part (conj path :helmet) default/helmet])}
                   (not torse?) {:title "Torse"
                                 :handler #(state/dispatch-on-event
                                            % [:add-helm-part (conj path :torse) default/torse])}
                   (not crest?) {:title "Crest"
                                 :handler #(state/dispatch-on-event
                                            % [:add-helm-part (conj path :crest) default/crest])})]

    {:title "Helm"
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title "Add"
                  :menu add-menu}])
     :nodes []}))

(defmethod ui-interface/component-form-data :heraldry.component/helm [_path]
  {:form form})
