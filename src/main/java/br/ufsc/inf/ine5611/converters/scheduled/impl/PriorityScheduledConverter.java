package br.ufsc.inf.ine5611.converters.scheduled.impl;

import br.ufsc.inf.ine5611.converters.Converter;
import br.ufsc.inf.ine5611.converters.scheduled.ConverterTask;
import br.ufsc.inf.ine5611.converters.scheduled.Priority;
import br.ufsc.inf.ine5611.converters.scheduled.ScheduledConverter;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.scene.input.KeyCode.T;

public class PriorityScheduledConverter implements ScheduledConverter, Comparator<ScheduledConverterTask> {
    public static final int DEFAULT_QUANTUM_LOW = 50;
    public static final int DEFAULT_QUANTUM_NORMAL = 100;
    public static final int DEFAULT_QUANTUM_HIGH = 200;
    public static long contador = 1;
    public Comparator<ScheduledConverterTask> comparador;
    public PriorityBlockingQueue queue = new PriorityBlockingQueue(11, this);
    
    public static HashMap<Priority, Integer> quanta = new HashMap<>();
    public Converter converter;
    public static ScheduledConverterTask tarefaAtual;

    public PriorityScheduledConverter(Converter converter) {
        //TODO implementar
        /* - Salve converter como um field, para uso posterior
           - Registre um listener em converter.addCompletionListener() para que você saiba
         *   quando uma tarefa terminou */
        this.converter = converter;
        this.converter.addCompletionListener(this::removeDaFila);
        
    }
    
    private void removeDaFila(ConverterTask converterTask){
        ScheduledConverterTask task = (ScheduledConverterTask) converterTask;
        task.complete(null);
        queue.remove(converterTask);
        tarefaAtual = (ScheduledConverterTask) queue.peek();
        try {
            this.processFor(getQuantum(tarefaAtual.getPriority()), MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(PriorityScheduledConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setQuantum(Priority priority, int milliseconds) {
        /* Dica: use um HasMap<Priority, Integer> para manter os quanta configurados para
         * cada prioridade */
        //TODO implementar
        quanta.put(priority, milliseconds);
    }

    @Override
    public int getQuantum(Priority priority) {
        /* Veja setQuantum */
        //TODO implementar
        return quanta.get(priority);
    }

    @Override
    public Collection<ConverterTask> getAllTasks() {
        /* Junte todas as tarefas não completas em um Collection */
        //TODO implementar
        return null;
    } 
    
    private void cancel(ConverterTask convertTask){
        converter.interrupt();
        convertTask.cancel(true);
        queue.remove(convertTask);
    }

    @Override
    public synchronized ConverterTask convert(InputStream inputStream, OutputStream outputStream,
                                String mediaType, long inputBytes, Priority priority) {
        /* - Crie um objeto ScheduledConverterTask utilizando os parâmetros dessa chamada
         * - Adicione o objeto em alguma fila (é possível implementar com uma ou várias filas)
         * - Se a nova tarefa for mais prioritária que a atualmente executando, interrompa */
        //TODO implementar
        ScheduledConverterTask novaTask = new ScheduledConverterTask(inputStream, 
                outputStream, this::cancel, mediaType, inputBytes, priority, contador++);
        queue.add(novaTask);
        try {
            if(queue.isEmpty()){
                tarefaAtual = novaTask;
                this.processFor(getQuantum(tarefaAtual.getPriority()), MILLISECONDS);             
            }
            else{
                ScheduledConverterTask tarefaPrioritaria = (ScheduledConverterTask) queue.peek();
                if(tarefaPrioritaria != tarefaAtual){
                    converter.interrupt();
                    tarefaAtual = tarefaPrioritaria;
                    this.processFor(getQuantum(tarefaAtual.getPriority()), MILLISECONDS);
                }
            }
        }  catch (InterruptedException ex) {
            Logger.getLogger(PriorityScheduledConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return novaTask;
    }

    @Override
    public void processFor(long interval, TimeUnit timeUnit) throws InterruptedException {
        /* Pseudocódigo:
        * interval = tempo_estourado
         * while (!tempo_estourado) {
         *   t = escolha_tarefa();
         *   t.incCycles();
         *   this.converter.processFor(getQuantum(t.getPriority()), timeUnit);
         * }
         */
        //TODO implementar
        long tempoEstourado = interval;
        Date date = new Date();
        while(tempoEstourado > 0){
            tarefaAtual.incCycles();
            try {
                this.converter.processFor(tarefaAtual, tempoEstourado, timeUnit);
                tempoEstourado--;
                Date newDate = new Date();
                System.out.println(date.getTime() - newDate.getTime());
                date = newDate;
            } catch (IOException ex) {
                Logger.getLogger(PriorityScheduledConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }

    @Override
    public synchronized void close() throws Exception {
        /* - Libere quaisquer recursos alocados
         * - Cancele as tarefas não concluídas
         */
        //TODO implementar
    }

    @Override
    public int compare(ScheduledConverterTask task1, ScheduledConverterTask task2) {
        //-1 == prioridade task nova é maior que prioridade task atual
        //0 == prioridade task atual é igual a prioridade task nova
        //1 == prioridade task nova é maior que prioridade task atual
        Priority prioridadeTask1 = task1.getPriority();
        Priority prioridadeTask2 = task2.getPriority();
        if(prioridadeTask1.equals(Priority.NORMAL) && prioridadeTask2.equals(Priority.HIGH)){
            return 1;
        }
        if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.HIGH)){
            return 1;
        }
        if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.NORMAL)){
            return 1;
        }
        if(prioridadeTask1.equals(prioridadeTask2)){
            return 0;
        }
        return -1;
    }
}
