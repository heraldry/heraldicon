(ns heraldicon.frontend.ui.element.blazonry-editor.editor
  (:require
   ["draft-js" :as draft-js]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.ui.element.blazonry-editor.editor-state :as editor-state]
   [heraldicon.frontend.ui.element.blazonry-editor.parser :as parser]
   [heraldicon.frontend.ui.element.blazonry-editor.shared :as shared]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private editor-state-path
  (conj shared/blazonry-editor-path :editor-state))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in editor-state-path (editor-state/create))
             (assoc-in last-parsed-path nil))
     :dispatch [::parser/clear-status]}))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [state (get-in db editor-state-path)
          text (editor-state/text state)
          last-parsed (get-in db last-parsed-path)]
      (if (not= text last-parsed)
        {:dispatch [::parser/parse text (editor-state/cursor-index state)]}
        {}))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-editor-state
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db editor-state-path state)
     ::debounce/dispatch [::debounce-update-editor-state [::parse-if-changed] change-dedupe-time]}))

(rf/reg-event-fx ::update-editor-state-and-parse
  (fn [{:keys [db]} [_ state]]
    {:db (assoc-in db editor-state-path state)
     :dispatch [::parse-if-changed]}))

(rf/reg-event-fx ::set-blazon
  (fn [{:keys [db]} [_ blazon]]
    (let [state (get-in db editor-state-path)]
      {:dispatch [::update-editor-state-and-parse (editor-state/set-text state blazon)]})))

#_(rf/reg-event-fx ::auto-completion-clicked
    (fn [{:keys [db]} [_ index cursor-index choice]]
      (let [state (get-in db editor-state-path)
            content ^draft-js/ContentState (.getCurrentContent state)
            current-text (.getPlainText content)
            choice (cond-> choice
                     (and (pos? index)
                          (pos? (count current-text))
                          (not= (subs current-text (dec index) index) " ")) (->> (str " "))
                     (and (< cursor-index (count current-text))
                          (not= (subs current-text cursor-index (inc cursor-index)) " ")) (str " "))
            {start-key :key
             start-offset :offset} (get-block-key-and-offset content index)
            {end-key :key
             end-offset :offset} (get-block-key-and-offset content cursor-index)
            range-selection {:anchorKey start-key
                             :anchorOffset start-offset
                             :focusKey end-key
                             :focusOffset end-offset}
            range-selection (-> state
                                ^draft-js/SelectionState (.getSelection)
                                (.merge (clj->js range-selection)))
            new-content (draft-js/Modifier.replaceText
                         content
                         range-selection
                         choice)
            new-state (-> state
                          (draft-js/EditorState.push
                           new-content
                           "insert-characters")
                          (put-cursor-at (+ (count choice) index)))]
        {:fx [[:dispatch [::auto-complete/clear]]
              [:dispatch [::update-editor-state new-state]]]})))

(rf/reg-event-fx ::on-parse-result
  (fn [{:keys [db]} [_ text {:keys [hdn index auto-complete]}]]
    {:db (-> db
             (update-in editor-state-path editor-state/highlight-unknown-string index)
             (assoc-in last-parsed-path text)
             (cond->
               hdn (assoc-in (conj hdn-path :coat-of-arms) {:field hdn})))
     :dispatch (if auto-complete
                 [::auto-complete/set auto-complete]
                 [::auto-complete/clear])}))

(defn editor []
  [:div {:style {:display "inline-block"
                 :outline "1px solid black"
                 :width "100%"
                 :height "100%"}}
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [editor-state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState (:state editor-state)
                            :onChange #(rf/dispatch-sync [::update-editor-state (editor-state/EditorState. %)])
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
