package gui;

import gui.wizard.GenericWizardPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import main.PropHandler;
import main.Settings;
import alg.Algorithm;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public class EmbedWizardPanel extends GenericWizardPanel
{
	JRadioButton embedButtons[];
	ButtonGroup group;

	ThreeDEmbedder embedder;
	boolean canProceed = false;

	public EmbedWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	protected int defaultSelection()
	{
		return 1;
	}

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getTitle()
	{
		return Settings.text("embed.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.desc");
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return ThreeDEmbedder.EMBEDDERS;
	}

	@Override
	protected boolean hasSimpleView()
	{
		return true;
	}

	class SimpleEmbedPanel extends SimplePanel
	{
		JRadioButton buttonYes = new JRadioButton(
				"Yes (recommended, applies '" + getDefaultEmbedder().getName() + "')", true);
		JRadioButton buttonNo = new JRadioButton("No");

		public SimpleEmbedPanel()
		{
			ButtonGroup group = new ButtonGroup();
			group.add(buttonYes);
			group.add(buttonNo);
			ActionListener a = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					updateSimpleSelection(e.getSource() == buttonYes);
				}
			};
			buttonYes.addActionListener(a);
			buttonNo.addActionListener(a);
			DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p"));
			b.setLineGapSize(Sizes.dluX(4));
			b.append(new JLabel("Embedd compounds according to their feature values into 3D space?"));
			b.nextLine();
			b.append(buttonYes);
			b.setLineGapSize(Sizes.dluX(4));
			b.nextLine();
			b.append(buttonNo);
			b.setBorder(new EmptyBorder(5, 0, 0, 0));

			setLayout(new BorderLayout());
			add(b.getPanel());

			String simpleSelected = PropHandler.get(getTitle() + "-simple-yes");
			if (simpleSelected != null && simpleSelected.equals("false"))
				buttonNo.setSelected(true);
		}

		private void updateSimpleSelection(boolean clusterYes)
		{
			wizard.update();
		}

		@Override
		protected Algorithm getAlgorithm()
		{
			if (buttonYes.isSelected())
				return getDefaultEmbedder();
			else
				return Random3DEmbedder.INSTANCE;
		}

		@Override
		protected void store()
		{
			PropHandler.put(getTitle() + "-simple-yes", buttonYes.isSelected() ? "true" : "false");
		}
	}

	@Override
	protected SimplePanel createSimpleView()
	{
		return new SimpleEmbedPanel();
	}

	private static ThreeDEmbedder DEFAULT = WekaPCA3DEmbedder.INSTANCE_NO_PROBS;

	public static ThreeDEmbedder getDefaultEmbedder()
	{
		return DEFAULT;
	}
}
