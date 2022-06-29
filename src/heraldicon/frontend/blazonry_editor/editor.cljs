(ns heraldicon.frontend.blazonry-editor.editor
  (:require
   ["draft-js" :as draft-js]
   [clojure.string :as s]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.blazonry-editor.dom :as dom]
   [heraldicon.frontend.blazonry-editor.parser :as parser]
   [heraldicon.frontend.blazonry-editor.shared :as shared]
   [heraldicon.frontend.blazonry-editor.state :as state]
   [heraldicon.frontend.blazonry-editor.suggestions :as suggestions]
   [heraldicon.frontend.debounce :as debounce]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private state-path
  (conj shared/blazonry-editor-path :state))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in state-path (state/create))
             (assoc-in last-parsed-path nil))
     :dispatch [::parser/clear-status]}))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [state (get-in db state-path)
          text (state/text state)
          last-parsed (get-in db last-parsed-path)]
      (if (not= text last-parsed)
        {:dispatch [::parser/parse text]}
        {}))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-state
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db state-path state)
     ::debounce/dispatch [::debounce-update-state [::parse-if-changed] change-dedupe-time]}))

(rf/reg-event-fx ::update-state-and-parse
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db state-path state)
     :dispatch [::parse-if-changed]}))

(rf/reg-event-fx ::set-blazon
  (fn [{:keys [db]} [_ blazon]]
    (let [state (get-in db state-path)]
      {:dispatch [::update-state-and-parse (state/set-text state blazon)]})))

(rf/reg-event-fx ::auto-completion-clicked
  (fn [{:keys [db]} [_ index cursor-index choice]]
    (let [state (get-in db state-path)]
      {:dispatch-n [[::auto-complete/clear]
                    [::update-state-and-parse (state/replace-text state index cursor-index choice)]]})))

(rf/reg-event-fx ::on-parse-result
  (fn [{:keys [db]} [_ text {:keys [hdn index suggestions]}]]
    (let [state (get-in db state-path)
          cursor-index (-> (state/cursor-index state)
                           (max index)
                           (min (count text)))
          substring-since-error (s/trim (subs text index cursor-index))]
      {:db (-> db
               (update-in state-path state/highlight-unknown-string index)
               (assoc-in last-parsed-path text)
               (cond->
                 hdn (assoc-in (conj hdn-path :coat-of-arms) {:field hdn})))
       :dispatch [::suggestions/set (dom/caret-position index) suggestions substring-since-error
                  (fn [choice]
                    (rf/dispatch [::auto-completion-clicked index cursor-index choice]))]})))

(defn editor []
  [:div {:style {:display "inline-block"
                 :outline "1px solid black"
                 :width "100%"
                 :height "100%"}}
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [state @(rf/subscribe [:get state-path])]
                          [:> draft-js/Editor
                           {:editorState state
                            :onChange #(rf/dispatch-sync [::update-state %])
                            :keyBindingFn (fn [event]
                                            (if (= (.-code event) "Tab")
                                              (do
                                                (.preventDefault event)
                                                (.stopPropagation event)
                                                "auto-complete")
                                              (draft-js/getDefaultKeyBinding event)))
                            :handleKeyCommand (fn [command]
                                                (if (= command "auto-complete")
                                                  (do
                                                    (rf/dispatch [::auto-complete/apply-first])
                                                    "handled")
                                                  "not-handled"))}]))})]])
